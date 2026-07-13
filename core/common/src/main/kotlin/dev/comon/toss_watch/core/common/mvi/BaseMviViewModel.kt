package dev.comon.toss_watch.core.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 모든 feature ViewModel의 MVI 베이스.
 *
 * - [uiState]: 화면이 구독하는 단일 불변 상태 스트림 (UDF)
 * - [sideEffect]: 네비게이션/토스트 등 재생되지 않는 1회성 이벤트 스트림
 * - [handleIntent]: View가 전달한 사용자 Intent의 단일 진입점
 */
abstract class BaseMviViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(
    initialState: S,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<E>()
    val sideEffect = _sideEffect.asSharedFlow()

    abstract fun handleIntent(intent: I)

    protected fun updateState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    protected fun sendSideEffect(effect: E) {
        viewModelScope.launch { _sideEffect.emit(effect) }
    }
}
