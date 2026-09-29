package dev.comon.watch_app.presentation.alarm

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.watch_app.domain.model.StockQuote

/**
 * @property opened false면 “정보가 도착했습니다” 알람 화면(울리는 중), true면 시세 화면.
 * @property alarmVersion 알람 화면이 떠 있는 동안 새 알람이 발화할 때마다 증가 — 시세 화면의 이미지 Scene 재생 트리거.
 */
data class StockAlarmUiState(
    val stockCode: String = "",
    val stockName: String = "",
    val opened: Boolean = false,
    val quote: StockQuoteState = StockQuoteState.Loading,
    val alarmVersion: Int = 0,
) : UiState

sealed interface StockQuoteState {
    data object Loading : StockQuoteState
    data class Loaded(val quote: StockQuote) : StockQuoteState
    data object Error : StockQuoteState
}

sealed interface StockAlarmIntent : UiIntent {
    data class NewAlarm(val stockCode: String, val stockName: String) : StockAlarmIntent
    data object Open : StockAlarmIntent
    data object Retry : StockAlarmIntent
    data object Dismiss : StockAlarmIntent
}

sealed interface StockAlarmEffect : UiSideEffect {
    /** 사용자가 알람을 확인했다 — 진동과 알람 알림을 멈춘다. */
    data object StopRinging : StockAlarmEffect
    data object Finish : StockAlarmEffect
}
