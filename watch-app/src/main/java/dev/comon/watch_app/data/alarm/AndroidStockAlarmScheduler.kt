package dev.comon.watch_app.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.watch_app.domain.repository.StockAlarmScheduler
import dev.comon.watch_app.presentation.MainActivity
import dev.comon.watch_app.service.StockAlarmReceiver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * `setAlarmClock`으로 예약한다 — Doze에서도 정시에 발화하고, 시스템에 “알람 시계” 알람으로
 * 표시된다. 매니페스트의 USE_EXACT_ALARM(API 33+) / SCHEDULE_EXACT_ALARM(API 31~32)이 필요하다.
 * 예약한 알람 id는 DataStore에 남겨, 폰에서 삭제된 알람의 예약을 프로세스 재시작 후에도 해제할 수 있게 한다.
 */
@Singleton
class AndroidStockAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DataStore<Preferences>,
) : StockAlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun scheduledIds(): Set<Long> =
        preferences.data.first()[SCHEDULED_IDS].orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    override suspend fun schedule(alarm: WatchAlarm, triggerAtMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission missing; alarm ${alarm.id} not scheduled")
            return
        }
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent()),
            fireIntent(alarm.id),
        )
        preferences.edit { it[SCHEDULED_IDS] = it[SCHEDULED_IDS].orEmpty() + alarm.id.toString() }
    }

    override suspend fun cancel(alarmId: Long) {
        alarmManager.cancel(fireIntent(alarmId))
        preferences.edit { it[SCHEDULED_IDS] = it[SCHEDULED_IDS].orEmpty() - alarmId.toString() }
    }

    // 알람마다 data URI를 달리해 PendingIntent를 구분한다(extras는 PendingIntent 동일성 판단에서 무시됨).
    private fun fireIntent(alarmId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, StockAlarmReceiver::class.java)
            .setAction(StockAlarmReceiver.ACTION_FIRE)
            .setData(Uri.parse("tosswatch://alarm/$alarmId"))
            .putExtra(StockAlarmReceiver.EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    // 시스템의 “다음 알람” 표시를 누르면 알람 목록 화면(앱 홈)을 연다.
    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "StockAlarmScheduler"
        val SCHEDULED_IDS = stringSetPreferencesKey("stock_alarm_scheduled_ids")
    }
}
