package dev.comon.watch_app.presentation.alarm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.watch_app.domain.usecase.FetchStockQuoteUseCase
import dev.comon.watch_app.domain.usecase.GetCustomAlarmImagePathsUseCase
import dev.comon.watch_app.service.StockAlarmNotifications
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 알람 화면이 뜨는 즉시 시세를 미리 요청해, 사용자가 알람을 누르는 시점엔 대부분 결과가 준비돼 있게 한다.
 * 응답 전에 누르면 [StockQuoteState.Loading]이 프로그래스바로 보인다.
 *
 * 같은 시각에 여러 종목 알람이 울리면 뒤에 온 알람은 [StockAlarmUiState.pending]에 쌓이고,
 * 시세 화면의 “다음”으로 한 종목씩 이어서 보여준다.
 */
@HiltViewModel
class StockAlarmViewModel @Inject constructor(
    private val fetchStockQuote: FetchStockQuoteUseCase,
    private val getCustomAlarmImagePaths: GetCustomAlarmImagePathsUseCase,
    savedState: SavedStateHandle,
) : BaseMviViewModel<StockAlarmUiState, StockAlarmIntent, StockAlarmEffect>(
    StockAlarmUiState(
        alarmId = savedState[StockAlarmNotifications.EXTRA_ALARM_ID] ?: 0L,
        stockCode = savedState[StockAlarmNotifications.EXTRA_STOCK_CODE] ?: "",
        stockName = savedState[StockAlarmNotifications.EXTRA_STOCK_NAME] ?: "",
    ),
) {
    private val fetchJobs = mutableMapOf<Long, Job>()

    init {
        fetch(uiState.value.alarmId, uiState.value.stockCode)
        loadCustomImages()
    }

    override fun handleIntent(intent: StockAlarmIntent) {
        when (intent) {
            is StockAlarmIntent.NewAlarm -> onNewAlarm(intent)
            StockAlarmIntent.Open -> {
                updateState { copy(opened = true) }
                sendSideEffect(StockAlarmEffect.StopRinging(uiState.value.alarmId))
            }
            StockAlarmIntent.Next -> {
                val next = uiState.value.pending.firstOrNull() ?: return
                updateState {
                    copy(
                        alarmId = next.alarmId,
                        stockCode = next.stockCode,
                        stockName = next.stockName,
                        // 다음 종목은 “도착했습니다” 화면 없이 바로 이미지 → 시세로 이어진다.
                        opened = true,
                        quote = next.quote,
                        alarmVersion = alarmVersion + 1,
                        pending = pending.drop(1),
                    )
                }
                sendSideEffect(StockAlarmEffect.StopRinging(next.alarmId))
            }
            StockAlarmIntent.Retry -> with(uiState.value) {
                if (quote is StockQuoteState.Error) fetch(alarmId, stockCode)
            }
            StockAlarmIntent.Dismiss -> sendSideEffect(StockAlarmEffect.Finish)
        }
    }

    private fun onNewAlarm(alarm: StockAlarmIntent.NewAlarm) {
        val state = uiState.value
        when {
            // 이미 다 확인한 화면이 남아 있던 것 — 이어 붙이지 않고 새 알람으로 처음부터 다시 울린다.
            state.opened && !state.hasNext -> {
                fetchJobs.values.forEach(Job::cancel)
                fetchJobs.clear()
                updateState {
                    StockAlarmUiState(
                        alarmId = alarm.alarmId,
                        stockCode = alarm.stockCode,
                        stockName = alarm.stockName,
                        alarmVersion = alarmVersion + 1,
                        customImagePaths = customImagePaths,
                    )
                }
            }
            // 같은 알람이 알림의 fullScreenIntent와 리시버의 직접 실행으로 두 번 전달된 경우.
            alarm.alarmId in state.alarmIds -> return
            else -> updateState {
                copy(pending = pending + PendingStockAlarm(alarm.alarmId, alarm.stockCode, alarm.stockName))
            }
        }
        fetch(alarm.alarmId, alarm.stockCode)
        // 화면이 떠 있는 사이 폰에서 이미지를 바꿨을 수 있다.
        loadCustomImages()
    }

    private fun loadCustomImages() {
        viewModelScope.launch {
            val paths = getCustomAlarmImagePaths()
            updateState { copy(customImagePaths = paths) }
        }
    }

    private fun fetch(alarmId: Long, stockCode: String) {
        fetchJobs.remove(alarmId)?.cancel()
        if (stockCode.isBlank()) {
            setQuote(alarmId, StockQuoteState.Error)
            return
        }
        setQuote(alarmId, StockQuoteState.Loading)
        fetchJobs[alarmId] = viewModelScope.launch {
            val quote = when (val result = fetchStockQuote(stockCode)) {
                is NetworkResult.Success -> StockQuoteState.Loaded(result.data)
                is NetworkResult.ApiError, is NetworkResult.NetworkError -> StockQuoteState.Error
            }
            setQuote(alarmId, quote)
        }
    }

    // 응답이 도착했을 때 그 알람이 화면에 보이는 중일 수도, 아직 차례를 기다리는 중일 수도 있다.
    private fun setQuote(alarmId: Long, quote: StockQuoteState) {
        updateState {
            if (this.alarmId == alarmId) {
                copy(quote = quote)
            } else {
                copy(pending = pending.map { if (it.alarmId == alarmId) it.copy(quote = quote) else it })
            }
        }
    }
}
