package dev.comon.watch_app.presentation.onboarding

import android.os.Build
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.WatchPairingPayload
import dev.comon.watch_app.domain.usecase.CheckFcmTokenRegisteredUseCase
import dev.comon.watch_app.domain.usecase.GetFcmTokenUseCase
import dev.comon.watch_app.domain.usecase.GetOrCreateDeviceUuidUseCase
import dev.comon.watch_app.domain.usecase.IsPairedUseCase
import dev.comon.watch_app.domain.usecase.SavePairedStateUseCase
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class WatchOnboardingViewModel @Inject constructor(
    private val getOrCreateDeviceUuidUseCase: GetOrCreateDeviceUuidUseCase,
    private val getFcmTokenUseCase: GetFcmTokenUseCase,
    private val checkFcmTokenRegisteredUseCase: CheckFcmTokenRegisteredUseCase,
    private val isPairedUseCase: IsPairedUseCase,
    private val savePairedStateUseCase: SavePairedStateUseCase,
    private val qrCodeGenerator: QrCodeGenerator,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<WatchOnboardingUiState, WatchOnboardingUiIntent, WatchOnboardingUiSideEffect>(
    WatchOnboardingUiState(),
) {

    /** QR 표시 중 자동 등록 확인 루프. 폰이 등록을 마치는 즉시(다음 틱에) 화면을 전환하기 위함. */
    private var pollingJob: Job? = null

    init {
        loadPairingState(forceRefreshToken = false)
    }

    override fun handleIntent(intent: WatchOnboardingUiIntent) {
        when (intent) {
            WatchOnboardingUiIntent.LoadToken,
            WatchOnboardingUiIntent.RetryClicked,
            -> loadPairingState(forceRefreshToken = false)

            WatchOnboardingUiIntent.RefreshClicked -> loadPairingState(forceRefreshToken = true)

            WatchOnboardingUiIntent.CheckNowClicked -> checkNow()

            WatchOnboardingUiIntent.GenerateQrClicked -> generateQr()
        }
    }

    /**
     * 로컬에 연동 완료 상태가 저장되어 있으면 API 호출 없이 바로 [WatchOnboardingPhase.Paired]를 보여준다.
     * 아니면 UUID 발급(최초 1회) → FCM 토큰 조회 → 2-5 등록 여부 확인 순으로 진행한다.
     */
    private fun loadPairingState(forceRefreshToken: Boolean) {
        pollingJob?.cancel()
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(phase = WatchOnboardingPhase.Loading) }

            val uuid = getOrCreateDeviceUuidUseCase()
            val modelName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

            if (isPairedUseCase()) {
                updateState { copy(phase = WatchOnboardingPhase.Paired(uuid = uuid, modelName = modelName)) }
                return@launch
            }

            getFcmTokenUseCase(forceRefresh = forceRefreshToken)
                .onSuccess { token -> resolvePairingState(token, uuid, modelName) }
                .onFailure {
                    updateState { copy(phase = WatchOnboardingPhase.Error(DEFAULT_TOKEN_ERROR)) }
                }
        }
    }

    private suspend fun resolvePairingState(token: String, uuid: String, modelName: String) {
        when (val result = checkFcmTokenRegisteredUseCase(token)) {
            is NetworkResult.Success -> {
                if (result.data) {
                    transitionToPaired(uuid, modelName)
                } else {
                    showQr(token, uuid, modelName)
                    startPolling()
                }
            }

            is NetworkResult.ApiError,
            is NetworkResult.NetworkError,
            -> updateState { copy(phase = WatchOnboardingPhase.Error(DEFAULT_CHECK_ERROR)) }
        }
    }

    /** 연동 완료 상태로 전환하며, 다음 실행부터 API 호출을 건너뛸 수 있도록 로컬에 영속화한다. */
    private suspend fun transitionToPaired(uuid: String, modelName: String) {
        pollingJob?.cancel()
        savePairedStateUseCase(true)
        updateState {
            copy(phase = WatchOnboardingPhase.Paired(uuid = uuid, modelName = modelName), isCheckingNow = false)
        }
    }

    /**
     * 연동 완료 화면의 "QR 생성" 버튼 — 새 FCM 토큰을 발급해(기존 등록 정보 무효화) 바로 QR을 보여준다.
     * 새 토큰은 서버에 아직 등록되어 있지 않으므로 등록 여부 확인 없이 폴링부터 다시 시작한다.
     * 로컬 연동완료 플래그도 false로 되돌려, 재연동 도중 프로세스가 죽어도 다음 실행 시 QR 화면부터 이어간다.
     */
    private fun generateQr() {
        pollingJob?.cancel()
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(phase = WatchOnboardingPhase.Loading) }
            savePairedStateUseCase(false)

            val uuid = getOrCreateDeviceUuidUseCase()
            val modelName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

            getFcmTokenUseCase(forceRefresh = true)
                .onSuccess { token ->
                    showQr(token, uuid, modelName)
                    startPolling()
                }
                .onFailure {
                    updateState { copy(phase = WatchOnboardingPhase.Error(DEFAULT_TOKEN_ERROR)) }
                }
        }
    }

    private fun showQr(token: String, uuid: String, modelName: String) {
        val payload = WatchPairingPayload(fcmToken = token, uuid = uuid, modelName = modelName)
        val content = Json.encodeToString(WatchPairingPayload.serializer(), payload)
        val bitmap = qrCodeGenerator.generate(content, QR_SIZE_PX)
        updateState { copy(phase = WatchOnboardingPhase.Qr(bitmap)) }
    }

    /** QR이 떠 있는 동안 일정 간격으로 조용히 재확인 — 실패해도 QR 화면을 그대로 유지하고 다음 틱에 재시도한다. */
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(dispatcherProvider.io) {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (checkPairingSilently()) break
            }
        }
    }

    /** "지금 확인" 버튼 — 폴링 주기를 기다리지 않고 즉시 한 번 확인하고, 결과를 로딩/토스트로 알려준다. */
    private fun checkNow() {
        if (uiState.value.phase !is WatchOnboardingPhase.Qr || uiState.value.isCheckingNow) return
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isCheckingNow = true) }
            when (val outcome = checkPairingOnce()) {
                is PairingCheckOutcome.Paired -> transitionToPaired(outcome.uuid, outcome.modelName)

                PairingCheckOutcome.NotPaired -> {
                    updateState { copy(isCheckingNow = false) }
                    sendSideEffect(WatchOnboardingUiSideEffect.ShowToast(CHECK_NOW_NOT_PAIRED_MESSAGE))
                }

                PairingCheckOutcome.Failed -> {
                    updateState { copy(isCheckingNow = false) }
                    sendSideEffect(WatchOnboardingUiSideEffect.ShowToast(CHECK_NOW_FAILED_MESSAGE))
                }
            }
        }
    }

    /** @return 등록이 확인되어 [WatchOnboardingPhase.Paired]로 전환했으면 true. */
    private suspend fun checkPairingSilently(): Boolean {
        val outcome = checkPairingOnce()
        if (outcome is PairingCheckOutcome.Paired) {
            transitionToPaired(outcome.uuid, outcome.modelName)
            return true
        }
        return false
    }

    private suspend fun checkPairingOnce(): PairingCheckOutcome {
        val uuid = getOrCreateDeviceUuidUseCase()
        val modelName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val token = getFcmTokenUseCase(forceRefresh = false).getOrNull()
            ?: return PairingCheckOutcome.Failed

        return when (val result = checkFcmTokenRegisteredUseCase(token)) {
            is NetworkResult.Success ->
                if (result.data) {
                    PairingCheckOutcome.Paired(uuid = uuid, modelName = modelName)
                } else {
                    PairingCheckOutcome.NotPaired
                }

            is NetworkResult.ApiError,
            is NetworkResult.NetworkError,
            -> PairingCheckOutcome.Failed
        }
    }

    private sealed interface PairingCheckOutcome {
        data class Paired(val uuid: String, val modelName: String) : PairingCheckOutcome
        data object NotPaired : PairingCheckOutcome
        data object Failed : PairingCheckOutcome
    }

    companion object {
        // 화면 표시 크기가 커진 만큼(OnboardingScreen fillMaxWidth 0.9f) 확대해도 흐려지지 않도록 해상도를 높인다.
        private const val QR_SIZE_PX = 480
        private const val POLL_INTERVAL_MS = 5_000L
        const val DEFAULT_TOKEN_ERROR = "토큰을 불러오지 못했어요. 다시 시도해 주세요."
        const val DEFAULT_CHECK_ERROR = "연동 상태를 확인하지 못했어요. 다시 시도해 주세요."
        const val CHECK_NOW_NOT_PAIRED_MESSAGE = "아직 연동되지 않았어요. 폰에서 QR을 스캔해 주세요."
        const val CHECK_NOW_FAILED_MESSAGE = "확인에 실패했어요. 잠시 후 다시 시도해 주세요."
    }
}
