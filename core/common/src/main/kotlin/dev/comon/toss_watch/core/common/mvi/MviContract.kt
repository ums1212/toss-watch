package dev.comon.toss_watch.core.common.mvi

/** 화면의 불변 UI 상태. 반드시 Immutable data class로 구현할 것. */
interface UiState

/** 사용자 행동(클릭, 입력 등)을 표현하는 단방향 이벤트. */
interface UiIntent

/** 네비게이션, 토스트 등 상태로 유지되지 않는 1회성 이벤트. */
interface UiSideEffect
