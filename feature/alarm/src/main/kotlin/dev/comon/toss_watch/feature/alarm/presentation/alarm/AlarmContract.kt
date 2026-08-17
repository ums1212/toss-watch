package dev.comon.toss_watch.feature.alarm.presentation.alarm

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.core.model.CachedStock

/** 알림이 등록된 종목 1건 — 해당 종목에 등록된 알림 개수를 함께 보여준다. */
data class StockAlarmSummary(
    val stockCode: String,
    val stockName: String,
    val alarmCount: Int,
)

data class AlarmUiState(
    val stockAlarms: List<StockAlarmSummary> = emptyList(),
    /** 대시보드가 캐싱해 둔 보유 종목 — 알림 추가 시 종목 선택 다이얼로그의 후보 목록으로 쓰인다. */
    val portfolioStocks: List<CachedStock> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface AlarmUiIntent : UiIntent {

    /** 종목 항목 탭 — 해당 종목의 알림 목록(AlarmDetailScreen)으로 이동한다. */
    data class OnStockClicked(val stockCode: String, val stockName: String) : AlarmUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnErrorDismissed : AlarmUiIntent
}

sealed interface AlarmUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 AlarmDetailRoute를 백스택에 push한다. */
    data class NavigateToAlarmDetail(val stockCode: String, val stockName: String) : AlarmUiSideEffect
}
