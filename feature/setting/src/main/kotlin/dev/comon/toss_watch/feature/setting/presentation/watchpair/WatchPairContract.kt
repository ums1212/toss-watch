package dev.comon.toss_watch.feature.setting.presentation.watchpair

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState

data class WatchPairUiState(
    val hasCameraPermission: Boolean = false,
    val isRegistering: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface WatchPairUiIntent : UiIntent {

    /** 카메라 권한 요청 결과. */
    data class OnPermissionResult(val granted: Boolean) : WatchPairUiIntent

    /** 카메라 프리뷰가 QR(워치 FCM 토큰 원문)을 인식했을 때. */
    data class OnQrScanned(val token: String) : WatchPairUiIntent

    /** 에러 다이얼로그의 확인 버튼 — 스캔을 다시 시작한다. */
    data object OnRetry : WatchPairUiIntent

    /** 상단 앱바의 뒤로가기. */
    data object OnBackClicked : WatchPairUiIntent
}

sealed interface WatchPairUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 설정 화면으로 백스택을 pop한다. */
    data object NavigateBack : WatchPairUiSideEffect

    data class ShowToast(val message: String) : WatchPairUiSideEffect
}
