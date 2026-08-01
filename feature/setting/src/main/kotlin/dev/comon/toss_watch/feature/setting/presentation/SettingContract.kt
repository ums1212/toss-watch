package dev.comon.toss_watch.feature.setting.presentation

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo

data class SettingUiState(
    /** 연동 완료된 워치(기기명+UUID). `null`이면 미연동 — "QR로 워치 연동" 버튼을 노출한다. */
    val pairedWatch: PairedWatchInfo? = null,

    /** 게스트(더미 데이터 체험) 모드 여부 — true면 하단 버튼이 "로그인하기"로 바뀐다. */
    val isGuest: Boolean = false,
) : UiState

sealed interface SettingUiIntent : UiIntent {

    /** "QR로 워치 연동" 버튼 — WatchPairRoute로 이동해 QR 스캔을 시작한다. */
    data object OnPairWatchClicked : SettingUiIntent

    /** 상단 앱바의 뒤로가기. */
    data object OnBackClicked : SettingUiIntent

    /** 토스 API 키 재설정 버튼. */
    data object OnTossKeyClicked : SettingUiIntent

    /** 설정 화면 맨 아래 로그아웃 버튼. */
    data object OnLogoutClicked : SettingUiIntent
}

sealed interface SettingUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 백스택을 pop한다. */
    data object NavigateBack : SettingUiSideEffect

    /** :app 라우터가 수신해 토스 API 키 입력 화면으로 이동한다. */
    data object NavigateToTossKey : SettingUiSideEffect

    /** :app 라우터가 수신해 Wear OS QR 페어링 화면으로 이동한다. */
    data object NavigateToWatchPair : SettingUiSideEffect
}
