package dev.comon.watch_app.domain.usecase

import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.watch_app.domain.alarm.nextTriggerAt
import dev.comon.watch_app.domain.repository.StockAlarmScheduler
import dev.comon.watch_app.domain.repository.WatchAlarmRepository
import dev.comon.watch_app.domain.repository.WatchStockQuoteRepository
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 폰에서 동기화된 스냅샷을 기준으로 워치 로컬 알람 예약을 맞춘다(멱등).
 * 켜진 알람은 다음 회차로 (재)예약하고, 스냅샷에서 사라지거나 꺼진 알람은 예약을 해제한다.
 */
class RescheduleStockAlarmsUseCase @Inject constructor(
    private val alarms: WatchAlarmRepository,
    private val scheduler: StockAlarmScheduler,
) {
    suspend operator fun invoke(now: ZonedDateTime = ZonedDateTime.now()) {
        val next = alarms.observe().first().snapshot?.alarms.orEmpty().mapNotNull { alarm ->
            alarm.nextTriggerAt(now)?.let { alarm to it.toInstant().toEpochMilli() }
        }
        val nextIds = next.map { it.first.id }.toSet()
        (scheduler.scheduledIds() - nextIds).forEach { scheduler.cancel(it) }
        next.forEach { (alarm, triggerAt) -> scheduler.schedule(alarm, triggerAt) }
    }
}

class FindWatchAlarmUseCase @Inject constructor(private val alarms: WatchAlarmRepository) {
    suspend operator fun invoke(alarmId: Long): WatchAlarm? =
        alarms.observe().first().snapshot?.alarms?.firstOrNull { it.id == alarmId }
}

class FetchStockQuoteUseCase @Inject constructor(private val repository: WatchStockQuoteRepository) {
    suspend operator fun invoke(stockCode: String) = repository.fetch(stockCode)
}
