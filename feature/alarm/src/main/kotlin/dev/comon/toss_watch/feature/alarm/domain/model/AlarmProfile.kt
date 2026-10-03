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
) {
    /**
     * 같은 종목·시각이면서 요일이 하나라도 겹치는지 — 겹치면 같은 순간에 두 번 울린다.
     * 꺼진 알림도 포함해 비교한다(나중에 켜면 그대로 중복이 되므로).
     */
    fun conflictsWith(stockCode: String, hour: Int, minute: Int, daysOfWeek: List<Int>): Boolean =
        this.stockCode == stockCode &&
            this.hour == hour &&
            this.minute == minute &&
            this.daysOfWeek.any { it in daysOfWeek }

    companion object {
        /** 계정당 등록할 수 있는 알림 총 개수 상한. */
        const val MAX_COUNT = 100
    }
}
