package dev.comon.watch_app.presentation

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.toss_watch.core.model.watch.WatchAlarmSnapshot
import dev.comon.toss_watch.core.model.watch.WatchStock
import dev.comon.watch_app.domain.repository.WatchAlarmSyncState
import dev.comon.watch_app.presentation.alarm.StockAlarmScreen
import dev.comon.watch_app.presentation.alarmsettings.WatchAlarmDetailScreen
import dev.comon.watch_app.presentation.alarmsettings.WatchAlarmUiState
import dev.comon.watch_app.presentation.alarmsettings.WatchStockListScreen
import dev.comon.watch_app.presentation.onboarding.OnboardingScreen
import dev.comon.watch_app.presentation.onboarding.QrCodeGenerator
import dev.comon.watch_app.presentation.onboarding.WatchOnboardingPhase
import dev.comon.watch_app.presentation.onboarding.WatchOnboardingUiState
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Documentation captures: real composables on an emulator, using only fictional data. */
class ReadmeScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val state = WatchAlarmUiState(sync = WatchAlarmSyncState(snapshot = WatchAlarmSnapshot(
        uuid = "readme-demo", session = "readme-demo", available = true,
        stocks = listOf(WatchStock("005930", "삼성전자"), WatchStock("AAPL", "애플")),
        alarms = listOf(WatchAlarm(1, "005930", "삼성전자", 9, 30, listOf(0, 1, 2, 3, 4), true)),
    )))

    private fun capture(name: String) {
        compose.waitForIdle()
        SystemClock.sleep(800)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = File(instrumentation.targetContext.getExternalFilesDir(null), "readme-$name.png")
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        output.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    @Test fun pairingQr() {
        val qr = QrCodeGenerator().generate(
            """{"fcm_token":"readme-demo-not-a-real-token","uuid":"readme-demo","model_name":"Wear OS Emulator"}""", 480)
        compose.setContent { TosswatchTheme {
            OnboardingScreen({}, WatchOnboardingUiState(WatchOnboardingPhase.Qr(qr)), {}, {}, {}, {})
        } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        capture("watch-pairing")
    }

    @Test fun stockList() {
        compose.setContent { TosswatchTheme { WatchStockListScreen(state, {}, {}, {}) } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        capture("watch-stocks")
    }

    @Test fun alarmDetail() {
        compose.setContent { TosswatchTheme { WatchAlarmDetailScreen("005930", "삼성전자", state, {}, {}, {}) } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        capture("watch-detail")
    }

    @Test fun notification() {
        compose.setContent { TosswatchTheme { StockAlarmScreen("삼성전자", "72,500원", "+1.25%", 0, {}) } }
        compose.mainClock.advanceTimeBy(3_500)
        capture("watch-notification")
    }
}
