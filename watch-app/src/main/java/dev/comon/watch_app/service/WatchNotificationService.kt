package dev.comon.watch_app.service

import android.Manifest
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.comon.watch_app.presentation.alarm.StockAlarmActivity

class WatchNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 서버 측 워치 FCM 토큰 갱신은 현재 폰 앱의 수동 등록 화면(feature:setting)이 담당한다.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val stockName = data[EXTRA_STOCK_NAME] ?: return
        val currentPrice = data[EXTRA_CURRENT_PRICE].orEmpty()
        val changeRate = data[EXTRA_CHANGE_RATE].orEmpty()

        showFullScreenAlarm(stockName, currentPrice, changeRate)
    }

    private fun showFullScreenAlarm(
        stockName: String,
        currentPrice: String,
        changeRate: String,
    ) {
        val fullScreenIntent = Intent(this, StockAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_STOCK_NAME, stockName)
            putExtra(EXTRA_CURRENT_PRICE, currentPrice)
            putExtra(EXTRA_CHANGE_RATE, changeRate)
        }
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
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            stockName.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            options,
        )

        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(stockName)
            .setContentText("$currentPrice ($changeRate)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(VIBRATION_PATTERN)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // 종목별 고유 ID로 발행 — 같은 종목 재알림은 갱신되고, 다른 종목은 각각 쌓인다.
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(stockName.hashCode(), notification)
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_STOCK_NAME = "stock_name"
        const val EXTRA_CURRENT_PRICE = "price"
        const val EXTRA_CHANGE_RATE = "change_rate"

        // NotificationChannel 설정(진동 등)은 최초 생성 후 불변이므로, 채널 설정 변경 시
        // ID를 새로 바꿔야 기존 설치 기기에도 새 설정이 반영된다.
        private const val CHANNEL_ID = "stock_alarm_channel_v2"
        private const val CHANNEL_NAME = "주식 알림"

        // fullScreenIntent가 설정된 알림은 Wear OS 플랫폼이 자체적으로 진동/사운드를
        // 억제하는 것으로 확인돼(로그: WearServices StreamManagerCollectorListener가
        // shouldVibrate=false로 덮어씀), 채널 진동 설정과 별개로 StockAlarmActivity에서
        // 이 패턴으로 직접 Vibrator를 호출한다. 두 곳의 패턴을 동일하게 유지하기 위해 공유.
        internal val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500)
    }
}
