package dev.comon.watch_app.presentation.onboarding

import android.graphics.Bitmap
import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState

data class WatchOnboardingUiState(
    val phase: WatchOnboardingPhase = WatchOnboardingPhase.Loading,
    /** "지금 확인" 버튼으로 트리거한 단발성 확인이 진행 중인지 여부. 버튼에 로딩 표시를 하기 위함. */
    val isCheckingNow: Boolean = false,
) : UiState

/**
 * 온보딩 화면의 단계.
 * - [Loading]: UUID/토큰 발급 및 2-5 등록 여부 확인 중.
 * - [Qr]: 미연동 상태 — FCM 토큰+UUID+모델명을 담은 QR을 계속 표시.
 * - [Paired]: 폰과 연동 완료 — 모델명/UUID를 안내.
 * - [Error]: 토큰 발급 또는 서버 확인 실패.
 */
sealed interface WatchOnboardingPhase {
    data object Loading : WatchOnboardingPhase
    data class Qr(val bitmap: Bitmap) : WatchOnboardingPhase
    data class Paired(val uuid: String, val modelName: String) : WatchOnboardingPhase
    data class Error(val message: String) : WatchOnboardingPhase
}

sealed interface WatchOnboardingUiIntent : UiIntent {
    data object LoadToken : WatchOnboardingUiIntent
    data object RetryClicked : WatchOnboardingUiIntent
    data object RefreshClicked : WatchOnboardingUiIntent

    /** "지금 확인" 버튼 — 자동 폴링을 기다리지 않고 즉시 등록 여부를 재확인한다. */
    data object CheckNowClicked : WatchOnboardingUiIntent

    /**
     * 연동 완료 화면의 "QR 생성" 버튼 — 폰이 다른 워치와 재연동해 이 기기의 연동 정보가
     * 서버에서 이미 무효화됐을 수 있는 경우, 새 QR을 발급해 재연동 화면으로 되돌아간다.
     */
    data object GenerateQrClicked : WatchOnboardingUiIntent
}

sealed interface WatchOnboardingUiSideEffect : UiSideEffect {
    data class ShowToast(val message: String) : WatchOnboardingUiSideEffect
}
