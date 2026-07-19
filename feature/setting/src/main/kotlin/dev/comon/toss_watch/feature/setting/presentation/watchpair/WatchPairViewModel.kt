package dev.comon.toss_watch.feature.setting.presentation.watchpair

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.usecase.RegisterWatchTokenUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 워치 앱 온보딩 화면의 QR(FCM 토큰 원문)을 스캔해 자동으로 서버에 등록한다.
 * QR 페이로드는 JSON 래핑 없는 순수 토큰 문자열이므로 별도 파싱 없이 그대로 넘긴다.
 */
@HiltViewModel
class WatchPairViewModel @Inject constructor(
    private val registerWatchTokenUseCase: RegisterWatchTokenUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<WatchPairUiState, WatchPairUiIntent, WatchPairUiSideEffect>(WatchPairUiState()) {

    override fun handleIntent(intent: WatchPairUiIntent) {
        when (intent) {
            is WatchPairUiIntent.OnPermissionResult -> updateState {
                copy(hasCameraPermission = intent.granted)
            }

            is WatchPairUiIntent.OnQrScanned -> registerWatchToken(intent.token)

            WatchPairUiIntent.OnRetry -> updateState {
                copy(errorMessage = null)
            }

            WatchPairUiIntent.OnBackClicked ->
                sendSideEffect(WatchPairUiSideEffect.NavigateBack)
        }
    }

    private fun registerWatchToken(rawToken: String) {
        // 카메라 프레임마다 분석기가 돌아가므로 이미 처리 중인 스캔 결과는 무시한다.
        if (uiState.value.isRegistering) return

        val token = rawToken.trim()
        if (token.isEmpty()) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isRegistering = true, errorMessage = null) }

            when (val result = registerWatchTokenUseCase(token)) {
                is NetworkResult.Success -> {
                    updateState { copy(isRegistering = false) }
                    sendSideEffect(WatchPairUiSideEffect.ShowToast(TOAST_TOKEN_REGISTERED))
                    sendSideEffect(WatchPairUiSideEffect.NavigateBack)
                }

                else -> updateState {
                    copy(isRegistering = false, errorMessage = result.toErrorMessage())
                }
            }
        }
    }

    private fun NetworkResult<*>.toErrorMessage(): String? = when (this) {
        is NetworkResult.Success -> null
        is NetworkResult.ApiError -> message ?: DEFAULT_API_ERROR
        is NetworkResult.NetworkError -> DEFAULT_NETWORK_ERROR
    }

    companion object {
        const val DEFAULT_API_ERROR = "워치 연동에 실패했어요. 잠시 후 다시 시도해 주세요."
        const val DEFAULT_NETWORK_ERROR = "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        const val TOAST_TOKEN_REGISTERED = "워치 알림 토큰이 등록됐어요."
    }
}
