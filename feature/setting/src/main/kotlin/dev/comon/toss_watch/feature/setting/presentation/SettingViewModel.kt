package dev.comon.toss_watch.feature.setting.presentation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.feature.setting.domain.usecase.LogoutUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObserveGuestModeUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObservePairedWatchUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.SyncPairedWatchUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val observePairedWatchUseCase: ObservePairedWatchUseCase,
    private val syncPairedWatchUseCase: SyncPairedWatchUseCase,
    private val observeGuestModeUseCase: ObserveGuestModeUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val dispatcherProvider: DispatcherProvider,
) : BaseMviViewModel<SettingUiState, SettingUiIntent, SettingUiSideEffect>(SettingUiState()) {

    init {
        observePairedWatch()
        syncPairedWatch()
        observeGuestMode()
    }

    override fun handleIntent(intent: SettingUiIntent) {
        when (intent) {
            SettingUiIntent.OnPairWatchClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateToWatchPair)

            SettingUiIntent.OnBackClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateBack)

            SettingUiIntent.OnTossKeyClicked ->
                sendSideEffect(SettingUiSideEffect.NavigateToTossKey)

            SettingUiIntent.OnLogoutClicked -> logout()
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

    /** 게스트(더미 데이터 체험) 모드 여부를 구독해 로그아웃/로그인 전환 버튼 문구를 고른다. */
    private fun observeGuestMode() {
        viewModelScope.launch(dispatcherProvider.io) {
            observeGuestModeUseCase().collect { isGuest ->
                updateState { copy(isGuest = isGuest) }
            }
        }
    }

    /**
     * 로그아웃 — 로컬 세션 토큰(및 게스트 모드였다면 게스트 플래그)을 제거한다. :app 최상위 라우터가
     * [dev.comon.toss_watch.core.datastore.TokenStore.observeHasSession] 변화를 감지해 로그인 화면으로
     * 자동 전환하므로 이 화면에서 별도 네비게이션을 발생시키지 않는다. 게스트 모드의 "로그인하기"도
     * 동일한 인텐트([SettingUiIntent.OnLogoutClicked])와 동일한 로직을 그대로 재사용한다.
     */
    private fun logout() {
        viewModelScope.launch(dispatcherProvider.io) {
            logoutUseCase()
        }
    }
}
