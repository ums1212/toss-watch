package dev.comon.toss_watch.feature.setting.presentation

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile

data class SettingUiState(
    val configuredAlarms: List<AlarmProfile> = emptyList(),
    /** 대시보드가 캐싱해 둔 보유 종목 — 알림 추가 다이얼로그의 종목 선택지로 쓰인다. */
    val availableStocks: List<CachedStock> = emptyList(),
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

    /** "QR로 워치 연동" 버튼 — WatchPairRoute로 이동해 QR 스캔을 시작한다. */
    data object OnPairWatchClicked : SettingUiIntent

    /** 상단 앱바의 뒤로가기. */
    data object OnBackClicked : SettingUiIntent

    /** 토스 API 키 재설정 버튼. */
    data object OnTossKeyClicked : SettingUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnErrorDismissed : SettingUiIntent
}

sealed interface SettingUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 백스택을 pop한다. */
    data object NavigateBack : SettingUiSideEffect

    /** :app 라우터가 수신해 토스 API 키 입력 화면으로 이동한다. */
    data object NavigateToTossKey : SettingUiSideEffect

    /** :app 라우터가 수신해 Wear OS QR 페어링 화면으로 이동한다. */
    data object NavigateToWatchPair : SettingUiSideEffect

    data class ShowToast(val message: String) : SettingUiSideEffect
}
