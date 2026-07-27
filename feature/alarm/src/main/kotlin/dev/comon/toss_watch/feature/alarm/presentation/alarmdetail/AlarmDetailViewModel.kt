package dev.comon.toss_watch.feature.alarm.presentation.alarmdetail

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.usecase.AddAlarmProfileUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.DeleteAlarmProfileUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.ObserveAlarmProfilesUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.SetAlarmEnabledInCacheUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.ToggleAlarmProfileUseCase
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class AlarmDetailViewModel @Inject constructor(
    private val fetchAlarmProfilesUseCase: FetchAlarmProfilesUseCase,
    private val observeAlarmProfilesUseCase: ObserveAlarmProfilesUseCase,
    private val addAlarmProfileUseCase: AddAlarmProfileUseCase,
    private val toggleAlarmProfileUseCase: ToggleAlarmProfileUseCase,
    private val setAlarmEnabledInCacheUseCase: SetAlarmEnabledInCacheUseCase,
    private val deleteAlarmProfileUseCase: DeleteAlarmProfileUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<AlarmDetailUiState, AlarmDetailUiIntent, AlarmDetailUiSideEffect>(AlarmDetailUiState()) {

    /** 알람 ID별 토글 디바운스 Job — 연타 시 이전 요청을 취소하고 마지막 값만 서버에 반영한다. */
    private val toggleJobs = mutableMapOf<Long, Job>()

    init {
        observeAlarms()
        refreshAlarms()
    }

    override fun handleIntent(intent: AlarmDetailUiIntent) {
        when (intent) {
            is AlarmDetailUiIntent.OnAddAlarm ->
                addAlarm(intent.stockCode, intent.stockName, intent.hour, intent.minute, intent.daysOfWeek)

            is AlarmDetailUiIntent.OnToggleAlarm ->
                toggleAlarm(intent.alarmId, intent.enabled)

            is AlarmDetailUiIntent.OnDeleteAlarm ->
                deleteAlarm(intent.alarmId)

            AlarmDetailUiIntent.OnBackClicked ->
                sendSideEffect(AlarmDetailUiSideEffect.NavigateBack)

            AlarmDetailUiIntent.OnErrorDismissed -> updateState {
                copy(errorMessage = null)
            }
        }
    }

    /** 공유 캐시를 구독 — 진입 경로(대시보드/알림 탭)와 무관하게 항상 최신 상태를 반영한다. */
    private fun observeAlarms() {
        viewModelScope.launch(dispatcherProvider.io) {
            observeAlarmProfilesUseCase().collect { alarms ->
                updateState { copy(alarms = alarms) }
            }
        }
    }

    private fun refreshAlarms() {
        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isLoading = true, errorMessage = null) }

            when (val result = fetchAlarmProfilesUseCase()) {
                is NetworkResult.Success -> updateState { copy(isLoading = false) }
                else -> updateState { copy(isLoading = false, errorMessage = result.toErrorMessage()) }
            }
        }
    }

    private fun addAlarm(stockCode: String, stockName: String, hour: Int, minute: Int, daysOfWeek: List<Int>) {
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = addAlarmProfileUseCase(stockCode, stockName, hour, minute, daysOfWeek)) {
                is NetworkResult.Success -> {
                    updateState { copy(isSaving = false) }
                    sendSideEffect(AlarmDetailUiSideEffect.ShowToast(TOAST_ALARM_ADDED))
                }

                else -> updateState {
                    copy(isSaving = false, errorMessage = result.toErrorMessage())
                }
            }
        }
    }

    /**
     * 낙관적 업데이트: 스위치를 누르는 즉시 공유 캐시를 반영해 응답을 기다리지 않고 UI가 움직이게 한다.
     * 동일 알람에 대한 연타는 [TOGGLE_DEBOUNCE_MS] 안에 들어오면 이전 Job을 취소하고 마지막 값만
     * 서버에 반영해 불필요한 API 호출을 막는다. 실패 시에만 이전 값으로 되돌린다.
     */
    private fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        val previousEnabled = uiState.value.alarms
            .firstOrNull { it.id == alarmId }
            ?.isEnabled
            ?: return

        setAlarmEnabledInCacheUseCase(alarmId, enabled)

        toggleJobs[alarmId]?.cancel()
        toggleJobs[alarmId] = viewModelScope.launch(dispatcherProvider.io) {
            delay(TOGGLE_DEBOUNCE_MS)

            when (val result = toggleAlarmProfileUseCase(alarmId, enabled)) {
                is NetworkResult.Success -> Unit

                else -> {
                    setAlarmEnabledInCacheUseCase(alarmId, previousEnabled)
                    updateState { copy(errorMessage = result.toErrorMessage()) }
                }
            }

            toggleJobs.remove(alarmId)
        }
    }

    private fun deleteAlarm(alarmId: Long) {
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = deleteAlarmProfileUseCase(alarmId)) {
                is NetworkResult.Success -> {
                    updateState { copy(isSaving = false) }
                    sendSideEffect(AlarmDetailUiSideEffect.ShowToast(TOAST_ALARM_DELETED))
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
        const val DEFAULT_API_ERROR = "설정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요."
        const val DEFAULT_NETWORK_ERROR = "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        const val TOAST_ALARM_ADDED = "알림이 추가됐어요."
        const val TOAST_ALARM_DELETED = "알림이 삭제됐어요."
        const val TOGGLE_DEBOUNCE_MS = 500L
    }
}
