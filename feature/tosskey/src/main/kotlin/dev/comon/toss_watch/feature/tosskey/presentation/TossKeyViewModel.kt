package dev.comon.toss_watch.feature.tosskey.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.tosskey.domain.usecase.RegisterTossKeyUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class TossKeyViewModel @Inject constructor(
    private val registerTossKeyUseCase: RegisterTossKeyUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<TossKeyUiState, TossKeyUiIntent, TossKeyUiSideEffect>(TossKeyUiState()) {

    override fun handleIntent(intent: TossKeyUiIntent) {
        when (intent) {
            is TossKeyUiIntent.OnClientIdChanged -> updateState {
                copy(clientId = intent.value)
            }

            is TossKeyUiIntent.OnClientSecretChanged -> updateState {
                copy(clientSecret = intent.value)
            }

            TossKeyUiIntent.OnSubmit -> registerTossKey()

            TossKeyUiIntent.OnBackClicked ->
                sendSideEffect(TossKeyUiSideEffect.NavigateBack)

            TossKeyUiIntent.OnErrorDismissed -> updateState {
                copy(errorMessage = null)
            }
        }
    }

    private fun registerTossKey() {
        val clientId = uiState.value.clientId.trim()
        val clientSecret = uiState.value.clientSecret.trim()
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            updateState { copy(errorMessage = ERROR_EMPTY_FIELD) }
            return
        }
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = registerTossKeyUseCase(clientId, clientSecret)) {
                is NetworkResult.Success -> {
                    updateState { copy(isSaving = false) }
                    sendSideEffect(TossKeyUiSideEffect.ShowToast(TOAST_REGISTERED))
                    sendSideEffect(TossKeyUiSideEffect.NavigateBack)
                }

                else -> updateState {
                    copy(isSaving = false, errorMessage = result.toErrorMessage())
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
        const val DEFAULT_API_ERROR = "토스 API 키를 등록하지 못했어요. 잠시 후 다시 시도해 주세요."
        const val DEFAULT_NETWORK_ERROR = "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        const val ERROR_EMPTY_FIELD = "클라이언트 ID와 시크릿을 모두 입력해 주세요."
        const val TOAST_REGISTERED = "토스 API 키가 등록되었어요."
    }
}
