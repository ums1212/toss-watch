package dev.comon.toss_watch.feature.alarm.presentation.alarm

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.common.resources.StringProvider
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.R
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.ObserveAlarmProfilesUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val fetchAlarmProfilesUseCase: FetchAlarmProfilesUseCase,
    private val observeAlarmProfilesUseCase: ObserveAlarmProfilesUseCase,
    private val stringProvider: StringProvider,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<AlarmUiState, AlarmUiIntent, AlarmUiSideEffect>(AlarmUiState()) {

    init {
        observeAlarms()
        refreshAlarms()
    }

    override fun handleIntent(intent: AlarmUiIntent) {
        when (intent) {
            is AlarmUiIntent.OnStockClicked ->
                sendSideEffect(AlarmUiSideEffect.NavigateToAlarmDetail(intent.stockCode, intent.stockName))

            AlarmUiIntent.OnErrorDismissed -> updateState {
                copy(errorMessage = null)
            }
        }
    }

    /** 공유 캐시를 구독 — AlarmDetailScreen에서의 추가/토글/삭제가 재조회 없이 즉시 반영된다. */
    private fun observeAlarms() {
        viewModelScope.launch(dispatcherProvider.io) {
            observeAlarmProfilesUseCase().collect { alarms ->
                updateState { copy(stockAlarms = alarms.toStockAlarmSummaries()) }
            }
        }
    }

    private fun refreshAlarms() {
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isLoading = true, errorMessage = null) }

            when (val result = fetchAlarmProfilesUseCase()) {
                is NetworkResult.Success -> updateState { copy(isLoading = false) }
                else -> updateState { copy(isLoading = false, errorMessage = result.toErrorMessage()) }
            }
        }
    }

    /** 종목코드로 그룹핑해 종목별 알림 개수 요약을 만든다 — 종목명 오름차순. */
    private fun List<AlarmProfile>.toStockAlarmSummaries(): List<StockAlarmSummary> =
        groupBy { it.stockCode }
            .map { (stockCode, alarms) ->
                StockAlarmSummary(
                    stockCode = stockCode,
                    stockName = alarms.first().stockName,
                    alarmCount = alarms.size,
                )
            }
            .sortedBy { it.stockName }

    private fun NetworkResult<*>.toErrorMessage(): String? = when (this) {
        is NetworkResult.Success -> null
        is NetworkResult.ApiError -> message ?: stringProvider.getString(R.string.alarm_error_api)
        is NetworkResult.NetworkError -> stringProvider.getString(R.string.alarm_error_network)
    }
}
