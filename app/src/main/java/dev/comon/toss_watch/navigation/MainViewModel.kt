package dev.comon.toss_watch.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.datastore.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** 전역 인증 상태 — 최초 판별 전(LOADING)에는 스플래시를 유지한다. */
enum class SessionState {
    LOADING,
    LOGGED_OUT,

    /** 세션은 있으나 토스 API 키가 미등록 — 온보딩 화면으로 유도. */
    NEEDS_TOSS_KEY,
    LOGGED_IN,
}

/**
 * 최상위 라우터의 상태 홀더.
 * :core:datastore의 세션/토스 키 등록 스트림을 구독해 Auth/TossKey/Dashboard 루트 전환 신호를 제공한다.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    tokenStore: TokenStore,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> =
        combine(
            tokenStore.observeHasSession(),
            tokenStore.observeTossKeyRegistered(),
        ) { hasSession, hasTossKey ->
            when {
                !hasSession -> SessionState.LOGGED_OUT
                !hasTossKey -> SessionState.NEEDS_TOSS_KEY
                else -> SessionState.LOGGED_IN
            }
        }
            .flowOn(dispatcherProvider.io)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SessionState.LOADING,
            )
}
