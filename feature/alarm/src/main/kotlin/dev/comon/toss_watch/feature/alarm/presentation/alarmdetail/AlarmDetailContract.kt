package dev.comon.toss_watch.feature.alarm.presentation.alarmdetail

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile

data class AlarmDetailUiState(
    /**
     * 등록된 알림 프로필 전체(공유 캐시 그대로) — 화면은 진입 시 전달받은 stockCode로
     * 걸러서 표시한다. 다른 종목의 알림 CRUD가 이 상태에 섞여도 문제 없다.
     */
    val alarms: List<AlarmProfile> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface AlarmDetailUiIntent : UiIntent {

    /** 알람 추가 다이얼로그 확정 — 시각/요일 조합으로 프로필을 생성한다. */
    data class OnAddAlarm(
        val stockCode: String,
        val stockName: String,
        val hour: Int,
        val minute: Int,
        /** 알림을 울릴 요일. 0(월)~6(일), 오름차순. */
        val daysOfWeek: List<Int>,
    ) : AlarmDetailUiIntent

    /** 알람 행의 활성 스위치 토글. */
    data class OnToggleAlarm(
        val alarmId: Long,
        val enabled: Boolean,
    ) : AlarmDetailUiIntent

    /** 알람 행의 삭제 버튼 — 확인 다이얼로그에서 확정된 후 전달된다. */
    data class OnDeleteAlarm(
        val alarmId: Long,
    ) : AlarmDetailUiIntent

    /** 상단 앱바의 뒤로가기. */
    data object OnBackClicked : AlarmDetailUiIntent

    /** 에러 다이얼로그의 확인 버튼. */
    data object OnErrorDismissed : AlarmDetailUiIntent
}

sealed interface AlarmDetailUiSideEffect : UiSideEffect {

    /** :app 라우터가 수신해 백스택을 pop한다. */
    data object NavigateBack : AlarmDetailUiSideEffect

    data class ShowToast(val message: String) : AlarmDetailUiSideEffect
}
