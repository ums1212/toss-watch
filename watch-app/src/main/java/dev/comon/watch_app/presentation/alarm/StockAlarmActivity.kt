package dev.comon.watch_app.presentation.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import dev.comon.watch_app.service.StockAlarmNotifications

/**
 * 로컬 알람이 울릴 때 전체 화면 알림(fullScreenIntent)으로 뜨는 알람 화면.
 * “정보가 도착했습니다” 화면에서 진동을 울리다가, 사용자가 누르면 시세 화면으로 전환한다.
 */
@AndroidEntryPoint
class StockAlarmActivity : ComponentActivity() {

    private val viewModel: StockAlarmViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val stopRingingRunnable = Runnable { vibrator.cancel() }
    private var alarmId = 0L

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null)
        // 사용자가 닫기 전까지 화면이 꺼지지 않도록 유지 — 워치는 화면 자동 꺼짐 시간이 짧다.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        alarmId = intent.getLongExtra(StockAlarmNotifications.EXTRA_ALARM_ID, 0L)
        // 구성 변경으로 재생성된 경우엔 이미 울렸거나 사용자가 확인한 상태이므로 다시 울리지 않는다.
        if (savedInstanceState == null) startRinging()

        setContent {
            TosswatchTheme {
                LaunchedEffect(Unit) {
                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            StockAlarmEffect.StopRinging -> stopRinging()
                            StockAlarmEffect.Finish -> {
                                stopRinging()
                                finish()
                            }
                        }
                    }
                }
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                StockAlarmContent(state = state, onIntent = viewModel::handleIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // launchMode="singleInstance"라 화면이 떠 있는 동안 다른 알람이 울리면 onCreate가 아닌
        // 여기로 전달된다. 이전 알람 알림은 정리하고 새 알람으로 처음부터 다시 울린다.
        setIntent(intent)
        StockAlarmNotifications.cancel(this, alarmId)
        alarmId = intent.getLongExtra(StockAlarmNotifications.EXTRA_ALARM_ID, 0L)
        viewModel.handleIntent(
            StockAlarmIntent.NewAlarm(
                stockCode = intent.getStringExtra(StockAlarmNotifications.EXTRA_STOCK_CODE).orEmpty(),
                stockName = intent.getStringExtra(StockAlarmNotifications.EXTRA_STOCK_NAME).orEmpty(),
            ),
        )
        startRinging()
    }

    override fun onDestroy() {
        if (isFinishing) stopRinging()
        super.onDestroy()
    }

    // Wear OS 플랫폼이 fullScreenIntent가 있는 알림은 채널에 진동이 설정돼 있어도
    // shouldVibrate=false로 억제하는 것을 실기기 로그로 확인했다(WearServices
    // StreamManagerCollectorListener). 그래서 채널 진동에 기대지 않고 알람 화면이
    // 직접 진동을 반복 재생하고, 사용자가 반응하지 않으면 일정 시간 뒤 멈춘다.
    private fun startRinging() {
        handler.removeCallbacks(stopRingingRunnable)
        @Suppress("DEPRECATION")
        vibrator.vibrate(
            VibrationEffect.createWaveform(RINGING_PATTERN, 0),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
        handler.postDelayed(stopRingingRunnable, RINGING_TIMEOUT_MS)
    }

    private fun stopRinging() {
        handler.removeCallbacks(stopRingingRunnable)
        vibrator.cancel()
        StockAlarmNotifications.cancel(this, alarmId)
    }

    private companion object {
        val RINGING_PATTERN = StockAlarmNotifications.VIBRATION_PATTERN + longArrayOf(1000)
        const val RINGING_TIMEOUT_MS = 60_000L
    }
}

@Composable
private fun StockAlarmContent(
    state: StockAlarmUiState,
    onIntent: (StockAlarmIntent) -> Unit,
) {
    val onDismiss = { onIntent(StockAlarmIntent.Dismiss) }
    if (!state.opened) {
        StockAlarmRingingScreen(
            stockName = state.stockName,
            onOpenClick = { onIntent(StockAlarmIntent.Open) },
            onDismissClick = onDismiss,
        )
        return
    }
    when (val quote = state.quote) {
        StockQuoteState.Loading -> StockQuoteLoadingScreen(onDismissClick = onDismiss)
        StockQuoteState.Error -> StockQuoteErrorScreen(
            onRetryClick = { onIntent(StockAlarmIntent.Retry) },
            onDismissClick = onDismiss,
        )
        is StockQuoteState.Loaded -> StockAlarmScreen(
            stockName = quote.quote.stockName.ifBlank { state.stockName },
            currentPrice = quote.quote.price,
            changeRate = quote.quote.changeRate,
            alarmVersion = state.alarmVersion,
            onDismissClick = onDismiss,
        )
    }
}
