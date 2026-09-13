package dev.comon.watch_app.presentation

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.material3.Text
import dev.comon.watch_app.presentation.navigation.WatchSwipeSceneStrategy
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchSwipeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun previousScreenIsVisibleDuringDragAndCancelDoesNotPop() {
        val stack = mutableStateListOf(1, 2)
        compose.setContent {
            TosswatchTheme {
                NavDisplay(
                    backStack = stack,
                    modifier = Modifier.testTag("navigation"),
                    onBack = { if (stack.size > 1) stack.removeAt(stack.lastIndex) },
                    sceneStrategies = listOf(remember { WatchSwipeSceneStrategy<Int>() }),
                    popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                    entryProvider = entryProvider {
                        entry<Int> { page ->
                            Box(Modifier.fillMaxSize().background(if (page == 1) Color(0xFF205020) else Color(0xFF203060))
                                .testTag("page-$page"), contentAlignment = Alignment.Center) {
                                Text(if (page == 1) "Previous screen" else "Current screen")
                            }
                        }
                    },
                )
            }
        }
        val navigation = compose.onNodeWithTag("navigation")
        navigation.performTouchInput {
            down(Offset(width * 0.08f, height * 0.5f))
            moveTo(Offset(width * 0.2f, height * 0.5f), delayMillis = 200)
            moveTo(Offset(width * 0.85f, height * 0.5f), delayMillis = 500)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("page-1").assertIsDisplayed()
        compose.onNodeWithTag("page-2").assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(1, 2), stack.toList()) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        android.os.SystemClock.sleep(500) // Wait for the held-drag frame to be presented.
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "swipe-background.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        instrumentation.uiAutomation.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/${file.name}")
            .let { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }

        navigation.performTouchInput {
            moveTo(Offset(width * 0.08f, height * 0.5f), delayMillis = 700)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(1, 2), stack.toList()) }
        compose.onNodeWithTag("page-2").assertIsDisplayed()

        navigation.performTouchInput {
            swipe(Offset(width * 0.08f, height * 0.5f), Offset(width * 0.98f, height * 0.5f), durationMillis = 700)
        }
        compose.waitUntil(5_000) { stack.size == 1 }
        compose.onNodeWithTag("page-1").assertIsDisplayed()
        navigation.performTouchInput {
            swipe(Offset(width * 0.08f, height * 0.5f), Offset(width * 0.98f, height * 0.5f), durationMillis = 700)
        }
        compose.runOnIdle { assertEquals(listOf(1), stack.toList()) }
    }
}
