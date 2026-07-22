package dev.comon.toss_watch.feature.setting.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.domain.usecase.AddAlarmProfileUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.DeleteAlarmProfileUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.LogoutUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObservePairedWatchUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObservePortfolioStocksUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.SyncPairedWatchUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ToggleAlarmProfileUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val fetchAlarmProfilesUseCase: FetchAlarmProfilesUseCase,
    private val addAlarmProfileUseCase: AddAlarmProfileUseCase,
    private val toggleAlarmProfileUseCase: ToggleAlarmProfileUseCase,
    private val deleteAlarmProfileUseCase: DeleteAlarmProfileUseCase,
    private val observePortfolioStocksUseCase: ObservePortfolioStocksUseCase,
    private val observePairedWatchUseCase: ObservePairedWatchUseCase,
    private val syncPairedWatchUseCase: SyncPairedWatchUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<SettingUiState, SettingUiIntent, SettingUiSideEffect>(SettingUiState()) {

    init {
        loadAlarms()
        observePortfolioStocks()
        observePairedWatch()
        syncPairedWatch()
    }

    override fun handleIntent(intent: SettingUiIntent) {
        when (intent) {
            is SettingUiIntent.OnAddAlarm ->
                addAlarm(intent.stockCode, intent.hour, intent.minute)

            is SettingUiIntent.OnToggleAlarm ->
                toggleAlarm(intent.alarmId, intent.enabled)

            is SettingUiIntent.OnDeleteAlarm ->
                deleteAlarm(intent.alarmId)

            SettingUiIntent.OnPairWatchClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateToWatchPair)

            SettingUiIntent.OnBackClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateBack)

            SettingUiIntent.OnTossKeyClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateToTossKey)

            SettingUiIntent.OnErrorDismissed -> updateState {
                copy(errorMessage = null)
            }

            SettingUiIntent.OnLogoutClicked -> logout()
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

    /** 대시보드가 캐싱한 보유 종목을 구독 — API 재호출 없이 알림 추가 종목 후보를 최신 상태로 유지한다. */
    private fun observePortfolioStocks() {
        viewModelScope.launch(dispatcherProvider.io) {
            observePortfolioStocksUseCase().collect { stocks ->
                updateState { copy(availableStocks = stocks) }
            }
        }
    }

    /**
     * 연동 완료된 워치 정보를 구독한다. WatchPair 화면에서 등록 성공 후 pop 복귀해도
     * 로컬(core:datastore) 값이 갱신되는 즉시 반영되므로 별도 재진입 트리거가 필요 없다.
     */
    private fun observePairedWatch() {
        viewModelScope.launch(dispatcherProvider.io) {
            observePairedWatchUseCase().collect { pairedWatch ->
                updateState { copy(pairedWatch = pairedWatch) }
            }
        }
    }

    /**
     * 서버 워치 FCM 등록 상태를 재조회해 로컬 pairedWatch를 서버 기준으로 복원/정리한다.
     * 폰앱 재설치 등으로 로컬 값이 유실된 경우를 대비한 best-effort 호출 — 실패해도
     * [observePairedWatch]가 기존 로컬 값을 그대로 유지하므로 UI를 방해하지 않는다.
     */
    private fun syncPairedWatch() {
        viewModelScope.launch(dispatcherProvider.io) {
            syncPairedWatchUseCase()
        }
    }

    /**
     * 로그아웃 — 로컬 세션 토큰을 제거한다. :app 최상위 라우터가 [dev.comon.toss_watch.core.datastore.TokenStore.observeHasSession]
     * 변화를 감지해 로그인 화면으로 자동 전환하므로 이 화면에서 별도 네비게이션을 발생시키지 않는다.
     */
    private fun logout() {
        viewModelScope.launch(dispatcherProvider.io) {
            logoutUseCase()
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

    private fun deleteAlarm(alarmId: Long) {
        if (uiState.value.isSaving) return

        viewModelScope.launch(dispatcherProvider.io) {
            updateState { copy(isSaving = true, errorMessage = null) }

            when (val result = deleteAlarmProfileUseCase(alarmId)) {
                is NetworkResult.Success -> {
                    updateState {
                        copy(isSaving = false, configuredAlarms = configuredAlarms.removeById(alarmId))
                    }
                    sendSideEffect(SettingUiSideEffect.ShowToast(TOAST_ALARM_DELETED))
                }

                else -> updateState {
                    copy(isSaving = false, errorMessage = result.toErrorMessage())
                }
            }
        }
    }

    private fun List<AlarmProfile>.replaceById(updated: AlarmProfile): List<AlarmProfile> =
        map { if (it.id == updated.id) updated else it }

    private fun List<AlarmProfile>.removeById(alarmId: Long): List<AlarmProfile> =
        filterNot { it.id == alarmId }

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
    }
}
