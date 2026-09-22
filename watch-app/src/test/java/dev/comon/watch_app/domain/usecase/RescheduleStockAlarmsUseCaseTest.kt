package dev.comon.watch_app.domain.usecase

import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.toss_watch.core.model.watch.WatchAlarmRequest
import dev.comon.toss_watch.core.model.watch.WatchAlarmSnapshot
import dev.comon.watch_app.domain.alarm.ALARM_ZONE
import dev.comon.watch_app.domain.repository.StockAlarmScheduler
import dev.comon.watch_app.domain.repository.WatchAlarmRepository
import dev.comon.watch_app.domain.repository.WatchAlarmSyncState
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RescheduleStockAlarmsUseCaseTest {
    private val now = ZonedDateTime.of(2026, 9, 21, 8, 0, 0, 0, ALARM_ZONE)
    private val repository = FakeAlarmRepository()
    private val scheduler = FakeScheduler()
    private val reschedule = RescheduleStockAlarmsUseCase(repository, scheduler)

    private fun alarm(id: Long, enabled: Boolean = true) = WatchAlarm(
        id = id, stockCode = "005930", stockName = "삼성전자", hour = 9, minute = 0,
        days = listOf(0), enabled = enabled,
    )

    private fun snapshot(vararg alarms: WatchAlarm) {
        repository.state.value = WatchAlarmSyncState(
            snapshot = WatchAlarmSnapshot(uuid = "watch", available = true, alarms = alarms.toList()),
        )
    }

    @Test fun `schedules enabled alarms at their next trigger`() = runTest {
        snapshot(alarm(1), alarm(2, enabled = false))
        reschedule(now)
        val nineAm = ZonedDateTime.of(2026, 9, 21, 9, 0, 0, 0, ALARM_ZONE).toInstant().toEpochMilli()
        assertEquals(mapOf(1L to nineAm), scheduler.scheduled)
    }

    @Test fun `cancels alarms removed or disabled on the phone`() = runTest {
        snapshot(alarm(1), alarm(2))
        reschedule(now)
        snapshot(alarm(1), alarm(2, enabled = false))
        reschedule(now)
        assertEquals(setOf(1L), scheduler.scheduled.keys)
        snapshot()
        reschedule(now)
        assertEquals(emptySet<Long>(), scheduler.scheduled.keys)
    }

    @Test fun `unpaired watch has nothing scheduled`() = runTest {
        scheduler.scheduled[7] = 0
        repository.state.value = WatchAlarmSyncState()
        reschedule(now)
        assertEquals(emptySet<Long>(), scheduler.scheduled.keys)
    }

    private class FakeScheduler : StockAlarmScheduler {
        val scheduled = mutableMapOf<Long, Long>()
        override suspend fun scheduledIds() = scheduled.keys.toSet()
        override suspend fun schedule(alarm: WatchAlarm, triggerAtMillis: Long) { scheduled[alarm.id] = triggerAtMillis }
        override suspend fun cancel(alarmId: Long) { scheduled.remove(alarmId) }
    }

    private class FakeAlarmRepository : WatchAlarmRepository {
        val state = MutableStateFlow(WatchAlarmSyncState())
        override fun observe() = state
        override suspend fun refresh() = Unit
        override suspend fun submit(request: WatchAlarmRequest) = false
    }
}
