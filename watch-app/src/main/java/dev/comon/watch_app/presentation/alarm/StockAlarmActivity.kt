package dev.comon.watch_app.presentation.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import dev.comon.watch_app.service.WatchNotificationService

class StockAlarmActivity : ComponentActivity() {

    private var stockName by mutableStateOf("")
    private var currentPrice by mutableStateOf("")
    private var changeRate by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        // 사용자가 닫기 전까지 화면이 꺼지지 않도록 유지 — 워치는 화면 자동 꺼짐 시간이 짧다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        applyIntentExtras(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // launchMode="singleInstance"라 화면이 떠 있는 동안 새 FCM이 오면 onCreate가 아닌
        // 여기로 전달된다. 최신 종목 정보로 상태만 갱신해 Compose가 자동 재구성되게 한다.
        setIntent(intent)
        applyIntentExtras(intent)
    }

    private fun applyIntentExtras(intent: Intent) {
        stockName = intent.getStringExtra(WatchNotificationService.EXTRA_STOCK_NAME).orEmpty()
        currentPrice = intent.getStringExtra(WatchNotificationService.EXTRA_CURRENT_PRICE).orEmpty()
        changeRate = intent.getStringExtra(WatchNotificationService.EXTRA_CHANGE_RATE).orEmpty()
    }
}
