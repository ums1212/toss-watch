package dev.comon.watch_app.domain.repository

import dev.comon.toss_watch.core.model.watch.WatchAlarm

/** 워치 로컬 정확 알람 예약. 구현은 AlarmManager.setAlarmClock을 사용한다. */
interface StockAlarmScheduler {
    suspend fun scheduledIds(): Set<Long>
    suspend fun schedule(alarm: WatchAlarm, triggerAtMillis: Long)
    suspend fun cancel(alarmId: Long)
}
