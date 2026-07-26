package dev.comon.toss_watch.feature.alarm.domain.model

/**
 * 알림 스케줄 1건.
 *
 * @param daysOfWeek 알림을 울릴 요일. 0(월)~6(일), Python `date.weekday()` 기준, 오름차순.
 * @param disabledReason 서버가 자동으로 비활성화한 경우의 사유(예: 상장폐지). 비어 있으면 사용자가 직접 설정한 상태 그대로.
 */
data class AlarmProfile(
    val id: Long,
    val stockCode: String,
    val stockName: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<Int>,
    val isEnabled: Boolean,
    val disabledReason: String = "",
)
