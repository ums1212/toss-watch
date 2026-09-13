package dev.comon.watch_app.presentation

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.onboarding.*
import dev.comon.watch_app.presentation.alarmsettings.*
import dev.comon.watch_app.presentation.alarm.StockAlarmScreen
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WatchShapeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun checkButton(label: String, screenshot: String, index: Int) {
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
        compose.waitForIdle()
        SystemClock.sleep(500) // Allow the emulator compositor to present the scrolled frame.
        val button = compose.onNode(hasText(label) and hasClickAction()).assertIsDisplayed()
        val bounds = button.fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val cx = root.center.x
        val cy = root.center.y
        val radius = minOf(root.width, root.height) / 2f
        for (x in listOf(bounds.left, bounds.right)) {
            for (y in listOf(bounds.top, bounds.bottom)) {
                assertTrue("$label is outside round screen: $bounds / $root", (x-cx)*(x-cx)+(y-cy)*(y-cy) <= radius*radius)
            }
        }
        saveScreenshot(screenshot)
    }

    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = File(instrumentation.targetContext.getExternalFilesDir(null), "shape-$name.png")
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        // AGP uninstalls the test app after execution; preserve captures outside its data folder.
        instrumentation.uiAutomation.executeShellCommand("cp ${output.absolutePath} /sdcard/Download/${output.name}")
            .use { descriptor -> android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() } }
    }

    @Test fun qrActionsFitRoundScreen() = qrActions(1f)
    @Test fun qrActionsFitRoundScreenWithLargeText() = qrActions(1.3f)

    private fun qrActions(fontScale: Float) {
        val bitmap = QrCodeGenerator().generate("""{"fcm_token":"test-token","uuid":"test-uuid","model_name":"Wear emulator"}""", 480)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                TosswatchTheme {
                    OnboardingScreen({}, WatchOnboardingUiState(WatchOnboardingPhase.Qr(bitmap)), {}, {}, {}, {})
                }
            }
        }
        checkButton(text(R.string.onboarding_refresh), "refresh-$fontScale", 2)
        checkButton(text(R.string.onboarding_check_now), "check-$fontScale", 3)
    }

    @Test fun lastSettingsButtonFitsRoundScreen() {
        compose.setContent { TosswatchTheme { WatchStockListScreen(WatchAlarmUiState(), {}, {}, {}) } }
        checkButton(text(R.string.alarm_back), "settings-back", 4)
    }

    @Test fun alarmDismissRemainsReachableWithLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                TosswatchTheme { StockAlarmScreen("Samsung Electronics", "123,456,789", "+12.34%", 0, {}) }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text(R.string.stock_alarm_dismiss)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(R.string.stock_alarm_dismiss)).performScrollTo().assertIsDisplayed()
        saveScreenshot("alarm-dismiss")
    }
}
