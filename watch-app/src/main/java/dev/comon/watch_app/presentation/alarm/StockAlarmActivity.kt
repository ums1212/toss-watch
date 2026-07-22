package dev.comon.watch_app.presentation.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    // 새 FCM이 도착해 정보가 갱신될 때마다 증가시켜, 화면에 이미지 Scene부터 다시
    // 재생시키는 트리거로 StockAlarmScreen에 전달한다.
    private var alarmVersion by mutableStateOf(0)

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
                    alarmVersion = alarmVersion,
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
        alarmVersion++
        vibrate()
    }

    // Wear OS 플랫폼이 fullScreenIntent가 있는 알림은 채널에 진동이 설정돼 있어도
    // shouldVibrate=false로 억제하는 것을 실기기 로그로 확인했다(WearServices
    // StreamManagerCollectorListener). 그래서 채널 진동에 기대지 않고 화면이 뜰 때마다
    // 액티비티가 직접 진동을 재생한다.
    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(WatchNotificationService.VIBRATION_PATTERN, -1))
    }
}
