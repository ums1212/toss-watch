package dev.comon.watch_app.service

import android.Manifest
import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.alarm.StockAlarmActivity

/** 로컬 알람 발화 시 띄우는 전체 화면 알람 알림. */
object StockAlarmNotifications {
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_STOCK_CODE = "stock_code"
    const val EXTRA_STOCK_NAME = "stock_name"

    // NotificationChannel 설정(진동 등)은 최초 생성 후 불변이므로, 채널 설정 변경 시
    // ID를 새로 바꿔야 기존 설치 기기에도 새 설정이 반영된다.
    private const val CHANNEL_ID = "stock_alarm_channel_v3"
    private val LEGACY_CHANNEL_IDS = listOf("stock_alarm_channel_v2")
    private const val TAG = "StockAlarmNotifications"

    // fullScreenIntent가 설정된 알림은 Wear OS 플랫폼이 자체적으로 진동/사운드를
    // 억제하는 것으로 확인돼(로그: WearServices StreamManagerCollectorListener가
    // shouldVibrate=false로 덮어씀), 채널 진동 설정과 별개로 StockAlarmActivity에서
    // 이 패턴으로 직접 Vibrator를 호출한다. 두 곳의 패턴을 동일하게 유지하기 위해 공유.
    internal val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500)

    /** 알람 화면을 띄울 Intent. 알림의 fullScreenIntent와 리시버의 직접 실행이 같은 화면을 공유한다. */
    fun alarmIntent(context: Context, alarm: WatchAlarm): Intent =
        Intent(context, StockAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_STOCK_CODE, alarm.stockCode)
            putExtra(EXTRA_STOCK_NAME, alarm.stockName)
        }

    fun showRinging(context: Context, alarm: WatchAlarm) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; alarm ${alarm.id} cannot ring")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(context, manager)

        val alarmIntent = alarmIntent(context, alarm)
        // 화면이 꺼진 상태에서는 워치 OEM의 알림 패널(예: 삼성 One UI Watch sysui)이 우리 대신
        // 이 PendingIntent를 전송한다. Android 15+에서는 그런 대리 전송 시 발신자뿐 아니라
        // PendingIntent를 만든 이 앱도 백그라운드 액티비티 실행을 명시적으로 허용해야 한다
        // (creator opt-in). 이게 없으면 ActivityTaskManager가 BAL_BLOCK으로 조용히 막는다.
        val options = if (Build.VERSION.SDK_INT >= 35) {
            ActivityOptions.makeBasic()
                .setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                )
                .toBundle()
        } else {
            null
        }
        val alarmPendingIntent = PendingIntent.getActivity(
            context,
            notificationId(alarm.id),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            options,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.stockName)
            .setContentText(context.getString(R.string.stock_alarm_arrived, alarm.stockName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(VIBRATION_PATTERN)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(alarmPendingIntent)
        // Android 14+에서는 사용자가 전체 화면 알림 권한을 끌 수 있다. 그 경우 heads-up 알림으로만 울린다.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || manager.canUseFullScreenIntent()) {
            builder.setFullScreenIntent(alarmPendingIntent, true)
        } else {
            Log.w(TAG, "Full-screen intent not allowed; falling back to heads-up notification")
        }
        // 알람별 고유 ID로 발행 — 같은 알람은 갱신되고, 같은 시각의 다른 종목 알람은 각각 쌓인다.
        manager.notify(notificationId(alarm.id), builder.build())
    }

    fun cancel(context: Context, alarmId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(alarmId))
    }

    private fun notificationId(alarmId: Long) = alarmId.hashCode()

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        LEGACY_CHANNEL_IDS.forEach(manager::deleteNotificationChannel)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_stock_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }
}
