package dev.comon.watch_app.presentation.alarm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.watch_app.domain.usecase.FetchStockQuoteUseCase
import dev.comon.watch_app.service.StockAlarmNotifications
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 알람 화면이 뜨는 즉시 시세를 미리 요청해, 사용자가 알람을 누르는 시점엔 대부분 결과가 준비돼 있게 한다.
 * 응답 전에 누르면 [StockQuoteState.Loading]이 프로그래스바로 보인다.
 */
@HiltViewModel
class StockAlarmViewModel @Inject constructor(
    private val fetchStockQuote: FetchStockQuoteUseCase,
    savedState: SavedStateHandle,
) : BaseMviViewModel<StockAlarmUiState, StockAlarmIntent, StockAlarmEffect>(
    StockAlarmUiState(
        stockCode = savedState[StockAlarmNotifications.EXTRA_STOCK_CODE] ?: "",
        stockName = savedState[StockAlarmNotifications.EXTRA_STOCK_NAME] ?: "",
    ),
) {
    private var fetchJob: Job? = null

    init {
        fetch()
    }

    override fun handleIntent(intent: StockAlarmIntent) {
        when (intent) {
            is StockAlarmIntent.NewAlarm -> {
                updateState {
                    StockAlarmUiState(
                        stockCode = intent.stockCode,
                        stockName = intent.stockName,
                        alarmVersion = alarmVersion + 1,
                    )
                }
                fetch()
            }
            StockAlarmIntent.Open -> {
                updateState { copy(opened = true) }
                sendSideEffect(StockAlarmEffect.StopRinging)
            }
            StockAlarmIntent.Retry -> if (uiState.value.quote is StockQuoteState.Error) fetch()
            StockAlarmIntent.Dismiss -> sendSideEffect(StockAlarmEffect.Finish)
        }
    }

    private fun fetch() {
        fetchJob?.cancel()
        val stockCode = uiState.value.stockCode
        if (stockCode.isBlank()) {
            updateState { copy(quote = StockQuoteState.Error) }
            return
        }
        updateState { copy(quote = StockQuoteState.Loading) }
        fetchJob = viewModelScope.launch {
            val quote = when (val result = fetchStockQuote(stockCode)) {
                is NetworkResult.Success -> StockQuoteState.Loaded(result.data)
                is NetworkResult.ApiError, is NetworkResult.NetworkError -> StockQuoteState.Error
            }
            updateState { copy(quote = quote) }
        }
    }
}
