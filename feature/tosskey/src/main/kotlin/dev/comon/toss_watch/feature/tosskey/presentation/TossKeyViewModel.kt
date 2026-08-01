package dev.comon.toss_watch.feature.tosskey.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.common.resources.StringProvider
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.tosskey.R
import dev.comon.toss_watch.feature.tosskey.domain.usecase.RegisterTossKeyUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class TossKeyViewModel @Inject constructor(
    private val registerTossKeyUseCase: RegisterTossKeyUseCase,
    private val stringProvider: StringProvider,
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
            updateState { copy(errorMessage = stringProvider.getString(R.string.tosskey_error_empty_field)) }
            return
        }
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = registerTossKeyUseCase(clientId, clientSecret)) {
                is NetworkResult.Success -> {
                    updateState { copy(isSaving = false) }
                    sendSideEffect(
                        TossKeyUiSideEffect.ShowToast(stringProvider.getString(R.string.tosskey_toast_registered)),
                    )
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
        is NetworkResult.ApiError -> message ?: stringProvider.getString(R.string.tosskey_error_api)
        is NetworkResult.NetworkError -> stringProvider.getString(R.string.tosskey_error_network)
    }
}
