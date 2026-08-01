package dev.comon.toss_watch.feature.auth.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.common.resources.StringProvider
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.auth.R
import dev.comon.toss_watch.feature.auth.domain.usecase.EnterGuestModeUseCase
import dev.comon.toss_watch.feature.auth.domain.usecase.LoginWithGoogleUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val enterGuestModeUseCase: EnterGuestModeUseCase,
    private val stringProvider: StringProvider,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<AuthUiState, AuthUiIntent, AuthUiSideEffect>(AuthUiState()) {

    override fun handleIntent(intent: AuthUiIntent) {
        when (intent) {
            is AuthUiIntent.OnGoogleLoginClicked -> loginWithGoogle(intent.idToken)

            is AuthUiIntent.OnGoogleCredentialFailed -> updateState {
                copy(errorMessage = intent.message ?: stringProvider.getString(R.string.auth_error_credential))
            }

            AuthUiIntent.OnGuestLoginClicked -> enterGuestMode()

            AuthUiIntent.OnAuthErrorDismissed -> updateState {
                copy(errorMessage = null)
            }
        }
    }

    /**
     * 실 로그인과 동일하게 isLoading을 true로 유지한 채 반환한다 — 게스트 진입도 :app의
     * MainViewModel.sessionState(GUEST)가 감지해 루트를 교체하는 단일 소스 오브 트루스를
     * 그대로 따르므로, 여기서 직접 네비게이션하지 않고 전환이 반영될 때까지 로딩 오버레이로
     * 로그인 화면 재노출을 막는다.
     */
    private fun enterGuestMode() {
        if (uiState.value.isLoading) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isLoading = true, errorMessage = null) }
            enterGuestModeUseCase()
            updateState { copy(isLoggedIn = true) }
        }
    }

    private fun loginWithGoogle(idToken: String) {
        if (uiState.value.isLoading) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isLoading = true, errorMessage = null) }

            when (val result = loginWithGoogleUseCase(idToken)) {
                is NetworkResult.Success -> {
                    // 네비게이션은 여기서 트리거하지 않는다 — 토큰은 이미 Data 레이어에서
                    // 영속 저장이 끝난 뒤이므로, :app의 MainViewModel.sessionState가 이를
                    // 감지해 대시보드/토스키 루트로 전환한다(단일 소스 오브 트루스).
                    // isLoading은 그 전환이 반영될 때까지 true로 유지해 로딩 오버레이가
                    // 화면 전환 순간까지 이어지도록 한다(로그인 화면 재노출 방지).
                    updateState { copy(isLoggedIn = true) }
                }

                is NetworkResult.ApiError -> updateState {
                    copy(
                        isLoading = false,
                        errorMessage = result.message ?: stringProvider.getString(R.string.auth_error_api),
                    )
                }

                is NetworkResult.NetworkError -> updateState {
                    copy(isLoading = false, errorMessage = stringProvider.getString(R.string.auth_error_network))
                }
            }
        }
    }
}
