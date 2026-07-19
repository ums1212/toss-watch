package dev.comon.watch_app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        val totalBuyAmount = data[EXTRA_TOTAL_BUY_AMOUNT].orEmpty()

        showFullScreenAlarm(stockName, currentPrice, changeRate, totalBuyAmount)
    }

    private fun showFullScreenAlarm(
        stockName: String,
        currentPrice: String,
        changeRate: String,
        totalBuyAmount: String,
    ) {
        val fullScreenIntent = Intent(this, StockAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_STOCK_NAME, stockName)
            putExtra(EXTRA_CURRENT_PRICE, currentPrice)
            putExtra(EXTRA_CHANGE_RATE, changeRate)
            putExtra(EXTRA_TOTAL_BUY_AMOUNT, totalBuyAmount)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            stockName.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(stockName)
            .setContentText("$currentPrice ($changeRate)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_STOCK_NAME = "stock_name"
        const val EXTRA_CURRENT_PRICE = "current_price"
        const val EXTRA_CHANGE_RATE = "change_rate"
        const val EXTRA_TOTAL_BUY_AMOUNT = "total_buy_amount"

        private const val CHANNEL_ID = "stock_alarm_channel"
        private const val CHANNEL_NAME = "주식 알림"
        private const val NOTIFICATION_ID = 1001
    }
}
