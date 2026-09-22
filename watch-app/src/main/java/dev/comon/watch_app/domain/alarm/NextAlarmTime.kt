package dev.comon.watch_app.domain.alarm

import dev.comon.toss_watch.core.model.watch.WatchAlarm
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 알람 시각은 폰·워치 UI 모두 한국 시간(`alarm_timezone`) 기준으로 설정된다. */
val ALARM_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

/**
 * [now] 이후(초과) 가장 가까운 발화 시각. 꺼진 알람이나 유효한 요일이 없으면 null.
 * `days`는 0(월)~6(일) 규약이다.
 */
fun WatchAlarm.nextTriggerAt(now: ZonedDateTime, zone: ZoneId = ALARM_ZONE): ZonedDateTime? {
    if (!enabled || hour !in 0..23 || minute !in 0..59) return null
    val weekdays = days.filter { it in 0..6 }.map { DayOfWeek.of(it + 1) }.toSet()
    if (weekdays.isEmpty()) return null
    val local = now.withZoneSameInstant(zone)
    for (offset in 0L..7L) {
        val date = local.toLocalDate().plusDays(offset)
        if (date.dayOfWeek !in weekdays) continue
        val candidate = ZonedDateTime.of(date, LocalTime.of(hour, minute), zone)
        if (candidate.isAfter(local)) return candidate
    }
    return null
}
