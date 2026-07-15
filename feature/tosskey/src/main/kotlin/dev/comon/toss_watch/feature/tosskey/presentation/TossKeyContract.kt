package dev.comon.toss_watch.feature.tosskey.presentation

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState

data class TossKeyUiState(
    val clientId: String = "",
    val clientSecret: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface TossKeyUiIntent : UiIntent {

    data class OnClientIdChanged(val value: String) : TossKeyUiIntent

    data class OnClientSecretChanged(val value: String) : TossKeyUiIntent

    /** 등록 버튼. */
    data object OnSubmit : TossKeyUiIntent

    /** 설정 화면 등에서 진입했을 때의 상단 앱바 뒤로가기. */
    data object OnBackClicked : TossKeyUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnErrorDismissed : TossKeyUiIntent
}

sealed interface TossKeyUiSideEffect : UiSideEffect {

    /**
     * :app 라우터가 수신해 백스택을 pop한다.
     * 온보딩 루트(백스택 크기 1)에서는 [dev.comon.toss_watch.navigation.Navigator.goBack]이
     * no-op으로 가드되고, 대신 세션 스트림(토스 키 등록 여부) 변화를 감지한 반응형 라우터가
     * 대시보드로 전환한다. 설정 화면에서 진입한 경우에는 실제로 pop되어 설정으로 복귀한다.
     */
    data object NavigateBack : TossKeyUiSideEffect

    data class ShowToast(val message: String) : TossKeyUiSideEffect
}
