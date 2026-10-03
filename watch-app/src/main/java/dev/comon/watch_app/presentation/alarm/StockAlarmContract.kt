package dev.comon.watch_app.presentation.alarm

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.watch_app.domain.model.StockQuote

/**
 * @property opened false면 “정보가 도착했습니다” 알람 화면(울리는 중), true면 시세 화면.
 * @property alarmVersion 화면에 보이는 종목이 바뀔 때마다(새 알람 발화, 다음 종목으로 이동) 증가 — 시세 화면의 이미지 Scene 재생 트리거.
 * @property customImagePaths 폰에서 설정한 사용자 지정 알람 이미지의 파일 경로. 없는 슬롯은 기본 이미지를 쓴다.
 * @property pending 같은 시각에 함께 울려, 지금 종목 다음에 차례로 보여줄 알람들.
 */
data class StockAlarmUiState(
    val alarmId: Long = 0L,
    val stockCode: String = "",
    val stockName: String = "",
    val opened: Boolean = false,
    val quote: StockQuoteState = StockQuoteState.Loading,
    val alarmVersion: Int = 0,
    val customImagePaths: Map<WatchAlarmImageSlot, String> = emptyMap(),
    val pending: List<PendingStockAlarm> = emptyList(),
) : UiState {
    val hasNext: Boolean get() = pending.isNotEmpty()

    /** 이 화면이 맡고 있는 모든 알람 — 같은 알람이 중복 전달됐는지 가려낼 때 쓴다. */
    val alarmIds: List<Long> get() = listOf(alarmId) + pending.map { it.alarmId }
}

/** 차례를 기다리는 알람. 시세는 기다리는 동안 미리 받아 둔다. */
data class PendingStockAlarm(
    val alarmId: Long,
    val stockCode: String,
    val stockName: String,
    val quote: StockQuoteState = StockQuoteState.Loading,
)

sealed interface StockQuoteState {
    data object Loading : StockQuoteState
    data class Loaded(val quote: StockQuote) : StockQuoteState
    data object Error : StockQuoteState
}

sealed interface StockAlarmIntent : UiIntent {
    data class NewAlarm(val alarmId: Long, val stockCode: String, val stockName: String) : StockAlarmIntent
    data object Open : StockAlarmIntent
    /** 시세 화면에서 다음 종목으로 넘어간다. */
    data object Next : StockAlarmIntent
    data object Retry : StockAlarmIntent
    data object Dismiss : StockAlarmIntent
}

sealed interface StockAlarmEffect : UiSideEffect {
    /** 사용자가 [alarmId] 알람을 확인했다 — 진동과 그 알람의 알림을 멈춘다. */
    data class StopRinging(val alarmId: Long) : StockAlarmEffect
    data object Finish : StockAlarmEffect
}
