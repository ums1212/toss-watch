package dev.comon.toss_watch.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.datastore.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 전역 인증 상태 — 최초 판별 전(LOADING)에는 스플래시를 유지한다. */
enum class SessionState {
    LOADING,
    LOGGED_OUT,
    LOGGED_IN,
}

/**
 * 최상위 라우터의 상태 홀더.
 * :core:datastore의 세션 스트림을 구독해 Auth/Dashboard 루트 전환 신호를 제공한다.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    tokenStore: TokenStore,
    dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> =
        tokenStore.observeHasSession()
            .map { hasSession ->
                if (hasSession) SessionState.LOGGED_IN else SessionState.LOGGED_OUT
            }
            .flowOn(dispatcherProvider.io)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SessionState.LOADING,
            )
}
