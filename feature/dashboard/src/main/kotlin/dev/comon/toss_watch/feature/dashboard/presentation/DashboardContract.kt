package dev.comon.toss_watch.feature.dashboard.presentation

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio

data class DashboardUiState(
    val accounts: List<Account> = emptyList(),
    val selectedAccountSeq: Long? = null,
    val portfolio: Portfolio? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface DashboardUiIntent : UiIntent {

    /** Pull-to-Refresh 제스처. */
    data object OnRefreshTriggered : DashboardUiIntent

    /** 계좌목록 팝업에서 다른 계좌를 선택. */
    data class OnAccountSelected(val accountSeq: Long) : DashboardUiIntent

    /** 상단 앱바의 설정(알림 스케줄러) 아이콘. */
    data object OnSettingClicked : DashboardUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnErrorDismissed : DashboardUiIntent
}

sealed interface DashboardUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 SettingRoute를 백스택에 push한다. */
    data object NavigateToSetting : DashboardUiSideEffect
}
