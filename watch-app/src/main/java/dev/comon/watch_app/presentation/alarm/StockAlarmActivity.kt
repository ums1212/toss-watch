package dev.comon.watch_app.presentation.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import dev.comon.watch_app.service.WatchNotificationService

class StockAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        val stockName = intent.getStringExtra(WatchNotificationService.EXTRA_STOCK_NAME).orEmpty()
        val currentPrice = intent.getStringExtra(WatchNotificationService.EXTRA_CURRENT_PRICE).orEmpty()
        val changeRate = intent.getStringExtra(WatchNotificationService.EXTRA_CHANGE_RATE).orEmpty()

        setContent {
            TosswatchTheme {
                StockAlarmScreen(
                    stockName = stockName,
                    currentPrice = currentPrice,
                    changeRate = changeRate,
                    onDismissClick = { finish() },
                )
            }
        }
    }
}
