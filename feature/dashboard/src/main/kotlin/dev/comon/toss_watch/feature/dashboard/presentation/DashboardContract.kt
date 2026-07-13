package dev.comon.toss_watch.feature.dashboard.presentation

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker
import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets

data class DashboardUiState(
    val totalAssets: UserAssets? = null,
    val activeTickers: List<TargetTicker> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface DashboardUiIntent : UiIntent {

    /** Pull-to-Refresh 제스처. */
    data object OnRefreshTriggered : DashboardUiIntent

    /** 상단 앱바의 설정(알림 스케줄러) 아이콘. */
    data object OnSettingClicked : DashboardUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnErrorDismissed : DashboardUiIntent
}

sealed interface DashboardUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 SettingRoute를 백스택에 push한다. */
    data object NavigateToSetting : DashboardUiSideEffect
}
