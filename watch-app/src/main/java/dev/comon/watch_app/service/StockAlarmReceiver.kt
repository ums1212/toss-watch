package dev.comon.watch_app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.watch_app.di.StockAlarmEntryPoint
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager가 예약 시각에 보내는 브로드캐스트. 알람을 울리고 다음 회차를 예약한다. */
class StockAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val entryPoint = EntryPointAccessors.fromApplication(context, StockAlarmEntryPoint::class.java)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 예약 이후 폰에서 꺼지거나 삭제됐으면 울리지 않는다.
                entryPoint.findWatchAlarm()(alarmId)?.takeIf { it.enabled }?.let {
                    ring(context.applicationContext, it)
                }
                // 현재 분을 지난 것으로 간주해, 방금 울린 회차가 다시 예약되지 않게 한다.
                val afterThisMinute = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(59)
                entryPoint.rescheduleStockAlarms()(afterThisMinute)
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * 화면이 꺼진 상태에서도 알람 앱처럼 곧바로 화면이 켜지고 알람 화면이 뜨게 한다.
     *
     * 1. `ACQUIRE_CAUSES_WAKEUP` 웨이크락으로 화면을 먼저 켠다. 기기가 자고 있으면 이것 없이는
     *    액티비티가 떠도 사용자가 볼 수 없다.
     * 2. 알림을 먼저 발행한다 — 3번이 시스템에 막혀도 알림은 남아 사용자가 눌러서 볼 수 있다.
     * 3. 액티비티를 직접 실행한다. 알림의 fullScreenIntent는 워치 OEM 알림 레이어(삼성 sysui 등)가
     *    대신 전송하다 BAL로 막히는 사례가 있어(WATCH_NOTIFICATION_INVESTIGATION.md), 우리 앱이
     *    정확 알람 수신 직후 직접 실행하는 경로를 함께 쓴다.
     */
    private fun ring(context: Context, alarm: WatchAlarm) {
        val power = context.getSystemService(PowerManager::class.java)
        @Suppress("DEPRECATION")
        val wakeLock = power.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            WAKE_LOCK_TAG,
        )
        // 액티비티가 뜨면 FLAG_KEEP_SCREEN_ON이 화면을 유지하므로, 여기서는 짧게만 잡는다.
        @Suppress("WakelockTimeout")
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        try {
            StockAlarmNotifications.showRinging(context, alarm)
            try {
                context.startActivity(StockAlarmNotifications.alarmIntent(context, alarm))
            } catch (e: SecurityException) {
                // 백그라운드 액티비티 실행이 막힌 기기 — 발행해 둔 알림이 폴백으로 남는다.
                Log.w(TAG, "Could not show the alarm screen directly", e)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    companion object {
        const val ACTION_FIRE = "dev.comon.watch_app.action.STOCK_ALARM_FIRE"
        const val EXTRA_ALARM_ID = StockAlarmNotifications.EXTRA_ALARM_ID
        private const val TAG = "StockAlarmReceiver"
        private const val WAKE_LOCK_TAG = "tosswatch:stock-alarm"
        private const val WAKE_LOCK_TIMEOUT_MS = 10_000L
    }
}
