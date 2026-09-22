package dev.comon.watch_app.domain.alarm

import dev.comon.toss_watch.core.model.watch.WatchAlarm
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextAlarmTimeTest {
    // 2026-09-21은 월요일.
    private fun seoul(day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 9, day, hour, minute, second, 0, ALARM_ZONE)

    private fun alarm(hour: Int = 9, minute: Int = 0, days: List<Int> = listOf(0), enabled: Boolean = true) =
        WatchAlarm(id = 1, stockCode = "005930", stockName = "삼성전자", hour = hour, minute = minute,
            days = days, enabled = enabled)

    @Test fun `later today when the time has not passed`() {
        assertEquals(seoul(21, 9, 0), alarm().nextTriggerAt(seoul(21, 8, 59)))
    }

    @Test fun `next week when the time today has passed`() {
        assertEquals(seoul(28, 9, 0), alarm().nextTriggerAt(seoul(21, 9, 0)))
    }

    @Test fun `skips to the next selected weekday`() {
        // 월(0) 알람 시각이 지났으면 수(2)로.
        assertEquals(seoul(23, 9, 0), alarm(days = listOf(0, 2)).nextTriggerAt(seoul(21, 10, 0)))
    }

    @Test fun `sunday rolls over to monday`() {
        // 2026-09-27은 일요일(6).
        assertEquals(seoul(28, 9, 0), alarm(days = listOf(0, 6)).nextTriggerAt(seoul(27, 9, 30)))
    }

    @Test fun `time is interpreted in korea time regardless of device zone`() {
        val utcNow = seoul(21, 8, 0).withZoneSameInstant(ZoneOffset.UTC)
        assertEquals(seoul(21, 9, 0).toInstant(), alarm().nextTriggerAt(utcNow)!!.toInstant())
    }

    @Test fun `disabled or empty alarms never fire`() {
        assertNull(alarm(enabled = false).nextTriggerAt(seoul(21, 8, 0)))
        assertNull(alarm(days = emptyList()).nextTriggerAt(seoul(21, 8, 0)))
        assertNull(alarm(days = listOf(9)).nextTriggerAt(seoul(21, 8, 0)))
    }

    @Test fun `after firing the same minute is not scheduled again`() {
        assertEquals(seoul(28, 9, 0), alarm().nextTriggerAt(seoul(21, 9, 0, 59)))
    }
}
