package dev.comon.toss_watch.feature.setting.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.domain.usecase.AddAlarmProfileUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.RegisterWatchTokenUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ToggleAlarmProfileUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val fetchAlarmProfilesUseCase: FetchAlarmProfilesUseCase,
    private val addAlarmProfileUseCase: AddAlarmProfileUseCase,
    private val toggleAlarmProfileUseCase: ToggleAlarmProfileUseCase,
    private val registerWatchTokenUseCase: RegisterWatchTokenUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<SettingUiState, SettingUiIntent, SettingUiSideEffect>(SettingUiState()) {

    init {
        loadAlarms()
    }

    override fun handleIntent(intent: SettingUiIntent) {
        when (intent) {
            is SettingUiIntent.OnAddAlarm ->
                addAlarm(intent.stockCode, intent.hour, intent.minute)

            is SettingUiIntent.OnToggleAlarm ->
                toggleAlarm(intent.alarmId, intent.enabled)

            is SettingUiIntent.OnFcmTokenChanged -> updateState {
                copy(fcmTokenInput = intent.value)
            }

            is SettingUiIntent.OnWatchTokenReceived -> updateState {
                if (fcmTokenInput.isBlank()) copy(fcmTokenInput = intent.token) else this
            }

            SettingUiIntent.OnFcmTokenSubmitted -> registerWatchToken()

            SettingUiIntent.OnBackClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateBack)

            SettingUiIntent.OnTossKeyClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateToTossKey)

            SettingUiIntent.OnErrorDismissed -> updateState {
                copy(errorMessage = null)
            }
        }
    }

    private fun loadAlarms() {
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isLoading = true, errorMessage = null) }

            when (val result = fetchAlarmProfilesUseCase()) {
                is NetworkResult.Success -> updateState {
                    copy(isLoading = false, configuredAlarms = result.data)
                }

                else -> updateState {
                    copy(isLoading = false, errorMessage = result.toErrorMessage())
                }
            }
        }
    }

    private fun addAlarm(stockCode: String, hour: Int, minute: Int) {
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = addAlarmProfileUseCase(stockCode, hour, minute)) {
                is NetworkResult.Success -> {
                    updateState {
                        copy(isSaving = false, configuredAlarms = configuredAlarms + result.data)
                    }
                    sendSideEffect(SettingUiSideEffect.ShowToast(TOAST_ALARM_ADDED))
                }

                else -> updateState {
                    copy(isSaving = false, errorMessage = result.toErrorMessage())
                }
            }
        }
    }

    private fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = toggleAlarmProfileUseCase(alarmId, enabled)) {
                is NetworkResult.Success -> updateState {
                    copy(
                        isSaving = false,
                        configuredAlarms = configuredAlarms.replaceById(result.data),
                    )
                }

                else -> updateState {
                    copy(isSaving = false, errorMessage = result.toErrorMessage())
                }
            }
        }
    }

    private fun registerWatchToken() {
        val token = uiState.value.fcmTokenInput.trim()
        if (token.isEmpty()) {
            updateState { copy(errorMessage = ERROR_EMPTY_TOKEN) }
            return
        }
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = registerWatchTokenUseCase(token)) {
                is NetworkResult.Success -> {
                    updateState { copy(isSaving = false) }
                    sendSideEffect(SettingUiSideEffect.ShowToast(TOAST_TOKEN_REGISTERED))
                }

                else -> updateState {
                    copy(isSaving = false, errorMessage = result.toErrorMessage())
                }
            }
        }
    }

    private fun List<AlarmProfile>.replaceById(updated: AlarmProfile): List<AlarmProfile> =
        map { if (it.id == updated.id) updated else it }

    private fun NetworkResult<*>.toErrorMessage(): String? = when (this) {
        is NetworkResult.Success -> null
        is NetworkResult.ApiError -> message ?: DEFAULT_API_ERROR
        is NetworkResult.NetworkError -> DEFAULT_NETWORK_ERROR
    }

    companion object {
        const val DEFAULT_API_ERROR = "설정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요."
        const val DEFAULT_NETWORK_ERROR = "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        const val ERROR_EMPTY_TOKEN = "등록할 워치 토큰을 입력해 주세요."
        const val TOAST_ALARM_ADDED = "알림이 추가됐어요."
        const val TOAST_TOKEN_REGISTERED = "워치 알림 토큰이 등록됐어요."
    }
}
