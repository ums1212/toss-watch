package dev.comon.watch_app.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class WatchOnboardingViewModel @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val qrCodeGenerator: QrCodeGenerator,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<WatchOnboardingUiState, WatchOnboardingUiIntent, WatchOnboardingUiSideEffect>(
    WatchOnboardingUiState(),
) {

    init {
        loadToken()
    }

    override fun handleIntent(intent: WatchOnboardingUiIntent) {
        when (intent) {
            WatchOnboardingUiIntent.LoadToken,
            WatchOnboardingUiIntent.RetryClicked,
            -> loadToken()
        }
    }

    private fun loadToken() {
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isLoading = true, errorMessage = null) }

            runCatching { firebaseMessaging.token.await() }
                .onSuccess { token ->
                    val bitmap = qrCodeGenerator.generate(token, QR_SIZE_PX)
                    updateState { copy(isLoading = false, qrBitmap = bitmap) }
                }
                .onFailure {
                    updateState { copy(isLoading = false, errorMessage = DEFAULT_TOKEN_ERROR) }
                }
        }
    }

    companion object {
        private const val QR_SIZE_PX = 300
        const val DEFAULT_TOKEN_ERROR = "토큰을 불러오지 못했어요. 다시 시도해 주세요."
    }
}
