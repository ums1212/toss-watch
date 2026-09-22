package dev.comon.watch_app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
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
                    StockAlarmNotifications.showRinging(context.applicationContext, it)
                }
                // 현재 분을 지난 것으로 간주해, 방금 울린 회차가 다시 예약되지 않게 한다.
                val afterThisMinute = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(59)
                entryPoint.rescheduleStockAlarms()(afterThisMinute)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "dev.comon.watch_app.action.STOCK_ALARM_FIRE"
        const val EXTRA_ALARM_ID = StockAlarmNotifications.EXTRA_ALARM_ID
    }
}
