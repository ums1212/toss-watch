package dev.comon.toss_watch.feature.setting.presentation

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile

data class SettingUiState(
    val configuredAlarms: List<AlarmProfile> = emptyList(),
    val fcmTokenInput: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface SettingUiIntent : UiIntent {

    /** 알람 추가 다이얼로그 확정 — 종목/시각 조합으로 프로필을 생성한다. */
    data class OnAddAlarm(
        val stockCode: String,
        val hour: Int,
        val minute: Int,
    ) : SettingUiIntent

    /** 알람 행의 활성 스위치 토글. */
    data class OnToggleAlarm(
        val alarmId: Long,
        val enabled: Boolean,
    ) : SettingUiIntent

    /** 토큰 입력란 변경. */
    data class OnFcmTokenChanged(val value: String) : SettingUiIntent

    /** SettingRoute.watchToken으로 전달된 페어링 토큰 — 입력란이 비어 있을 때만 프리필. */
    data class OnWatchTokenReceived(val token: String) : SettingUiIntent

    /** 토큰 등록 버튼. */
    data object OnFcmTokenSubmitted : SettingUiIntent

    /** 상단 앱바의 뒤로가기. */
    data object OnBackClicked : SettingUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnErrorDismissed : SettingUiIntent
}

sealed interface SettingUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 백스택을 pop한다. */
    data object NavigateBack : SettingUiSideEffect

    data class ShowToast(val message: String) : SettingUiSideEffect
}
