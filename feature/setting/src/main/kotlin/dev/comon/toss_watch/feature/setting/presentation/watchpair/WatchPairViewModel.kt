package dev.comon.toss_watch.feature.setting.presentation.watchpair

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.common.resources.StringProvider
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.WatchPairingPayload
import dev.comon.toss_watch.feature.setting.R
import dev.comon.toss_watch.feature.setting.domain.usecase.RegisterWatchTokenUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 워치 앱 온보딩 화면의 QR을 스캔해 자동으로 서버에 등록한다.
 * QR 페이로드는 [WatchPairingPayload] JSON(fcm_token+uuid+model_name)이며,
 * 이 ViewModel이 역직렬화까지 책임진다(카메라 프리뷰는 원문만 전달).
 */
@HiltViewModel
class WatchPairViewModel @Inject constructor(
    private val registerWatchTokenUseCase: RegisterWatchTokenUseCase,
    private val stringProvider: StringProvider,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<WatchPairUiState, WatchPairUiIntent, WatchPairUiSideEffect>(WatchPairUiState()) {

    private val json = Json { ignoreUnknownKeys = true }

    override fun handleIntent(intent: WatchPairUiIntent) {
        when (intent) {
            is WatchPairUiIntent.OnPermissionResult -> updateState {
                copy(
                    hasCameraPermission = intent.granted,
                    isPermissionPermanentlyDenied = !intent.granted && intent.permanentlyDenied,
                )
            }

            is WatchPairUiIntent.OnQrScanned -> handleQrScanned(intent.rawPayload)

            WatchPairUiIntent.OnRetry -> updateState {
                copy(errorMessage = null)
            }

            WatchPairUiIntent.OnBackClicked ->
                sendSideEffect(WatchPairUiSideEffect.NavigateBack)
        }
    }

    private fun handleQrScanned(rawPayload: String) {
        // 카메라 프레임마다 분석기가 돌아가므로 이미 처리 중인 스캔 결과는 무시한다.
        if (uiState.value.isRegistering) return

        val payload = runCatching {
            json.decodeFromString(WatchPairingPayload.serializer(), rawPayload.trim())
        }.getOrElse {
            if (it is SerializationException || it is IllegalArgumentException) {
                updateState { copy(errorMessage = stringProvider.getString(R.string.watchpair_error_invalid_qr)) }
                return
            }
            throw it
        }

        registerWatchToken(payload)
    }

    private fun registerWatchToken(payload: WatchPairingPayload) {
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isRegistering = true, errorMessage = null) }

            when (
                val result = registerWatchTokenUseCase(
                    fcmToken = payload.fcmToken,
                    uuid = payload.uuid,
                    modelName = payload.modelName,
                )
            ) {
                is NetworkResult.Success -> {
                    updateState { copy(isRegistering = false) }
                    sendSideEffect(
                        WatchPairUiSideEffect.ShowToast(stringProvider.getString(R.string.watchpair_toast_registered)),
                    )
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
        is NetworkResult.ApiError -> message ?: stringProvider.getString(R.string.watchpair_error_api)
        is NetworkResult.NetworkError -> stringProvider.getString(R.string.watchpair_error_network)
    }
}
