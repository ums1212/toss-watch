package dev.comon.watch_app.presentation.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
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
 * “정보가 도착했습니다” 화면에서 진동을 두 번 울리고, 사용자가 누르면 시세 화면으로 전환한다.
 */
@AndroidEntryPoint
class StockAlarmActivity : ComponentActivity() {

    private val viewModel: StockAlarmViewModel by viewModels()

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

        // 구성 변경으로 재생성된 경우엔 이미 울렸거나 사용자가 확인한 상태이므로 다시 울리지 않는다.
        if (savedInstanceState == null) startRinging()

        setContent {
            TosswatchTheme {
                LaunchedEffect(Unit) {
                    viewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is StockAlarmEffect.StopRinging -> stopRinging(effect.alarmId)
                            StockAlarmEffect.Finish -> {
                                stopCurrent()
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
        // 여기로 전달된다. 같은 시각의 다른 종목 알람이면 지금 종목 뒤에 이어 붙는다(StockAlarmViewModel).
        setIntent(intent)
        val before = viewModel.uiState.value
        viewModel.handleIntent(
            StockAlarmIntent.NewAlarm(
                alarmId = intent.getLongExtra(StockAlarmNotifications.EXTRA_ALARM_ID, 0L),
                stockCode = intent.getStringExtra(StockAlarmNotifications.EXTRA_STOCK_CODE).orEmpty(),
                stockName = intent.getStringExtra(StockAlarmNotifications.EXTRA_STOCK_NAME).orEmpty(),
            ),
        )
        // 이미 맡고 있는 알람이 중복 전달된 것이면 상태가 그대로다 — 다시 울리지 않는다.
        if (viewModel.uiState.value != before) startRinging()
    }

    override fun onDestroy() {
        if (isFinishing) stopCurrent()
        super.onDestroy()
    }

    // Wear OS 플랫폼이 fullScreenIntent가 있는 알림은 채널에 진동이 설정돼 있어도
    // shouldVibrate=false로 억제하는 것을 실기기 로그로 확인했다(WearServices
    // StreamManagerCollectorListener). 그래서 채널 진동에 기대지 않고 알람 화면이
    // 직접 진동을 재생한다. 계속 울리지 않도록 패턴(두 번 진동)을 반복 없이 한 번만 재생한다.
    private fun startRinging() {
        @Suppress("DEPRECATION")
        vibrator.vibrate(
            VibrationEffect.createWaveform(StockAlarmNotifications.VIBRATION_PATTERN, NO_REPEAT),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
    }

    private fun stopRinging(alarmId: Long) {
        vibrator.cancel()
        StockAlarmNotifications.cancel(this, alarmId)
    }

    // 화면을 닫을 때는 보고 있던 알람만 정리한다. 아직 차례가 오지 않은 알람은 알림으로 남아,
    // 사용자가 나중에 눌러서 볼 수 있다.
    private fun stopCurrent() {
        stopRinging(viewModel.uiState.value.alarmId)
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}

@Composable
private fun StockAlarmContent(
    state: StockAlarmUiState,
    onIntent: (StockAlarmIntent) -> Unit,
) {
    val onDismiss = { onIntent(StockAlarmIntent.Dismiss) }
    // 이어서 보여줄 종목이 남아 있을 때만 “다음” 버튼을 띄운다.
    val onNext = if (state.hasNext) ({ onIntent(StockAlarmIntent.Next) }) else null
    if (!state.opened) {
        StockAlarmRingingScreen(
            stockName = state.stockName,
            pendingCount = state.pending.size,
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
            onNextClick = onNext,
        )
        is StockQuoteState.Loaded -> StockAlarmScreen(
            stockName = quote.quote.stockName.ifBlank { state.stockName },
            currentPrice = quote.quote.price,
            changeRate = quote.quote.changeRate,
            alarmVersion = state.alarmVersion,
            onDismissClick = onDismiss,
            onNextClick = onNext,
            customImagePaths = state.customImagePaths,
        )
    }
}
