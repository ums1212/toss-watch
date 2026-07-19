package dev.comon.toss_watch.feature.auth.presentation

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
) : UiState

sealed interface AuthUiIntent : UiIntent {

    /** Credential Manager에서 구글 ID Token 확보에 성공했을 때. */
    data class OnGoogleLoginClicked(val idToken: String) : AuthUiIntent

    /** Credential Manager 단계에서 실패했을 때 (유저 취소는 제외하고 발행). */
    data class OnGoogleCredentialFailed(val message: String?) : AuthUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnAuthErrorDismissed : AuthUiIntent
}

sealed interface AuthUiSideEffect : UiSideEffect {

    data class ShowToast(val message: String) : AuthUiSideEffect
}
