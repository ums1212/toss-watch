package dev.comon.watch_app.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import dev.comon.toss_watch.core.common.coroutine.DefaultDispatcherProvider
import dev.comon.toss_watch.core.common.resources.DefaultStringProvider
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.WatchAlarmRequest
import dev.comon.watch_app.R
import dev.comon.watch_app.domain.repository.*
import dev.comon.watch_app.domain.usecase.*
import dev.comon.watch_app.presentation.alarmsettings.WatchAlarmViewModel
import dev.comon.watch_app.presentation.navigation.WatchNavHost
import dev.comon.watch_app.presentation.onboarding.*
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WatchNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val pairing = FakePairing()
    private val models = mutableListOf<WatchOnboardingViewModel>()
    private lateinit var alarms: WatchAlarmViewModel
    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun start() {
        val repository = object : WatchAlarmRepository {
            override fun observe() = flowOf(WatchAlarmSyncState())
            override suspend fun refresh() = Unit
            override suspend fun submit(request: WatchAlarmRequest) = false
        }
        alarms = WatchAlarmViewModel(ObserveWatchAlarmsUseCase(repository), RefreshWatchAlarmsUseCase(repository),
            SaveWatchAlarmUseCase(repository), SavedStateHandle())
        compose.setContent {
            TosswatchTheme {
                WatchNavHost(alarms, pairingViewModel = {
                    remember {
                        WatchOnboardingViewModel(GetOrCreateDeviceUuidUseCase(pairing), GetFcmTokenUseCase(pairing),
                            CheckFcmTokenRegisteredUseCase(pairing), IsPairedUseCase(pairing), SavePairedStateUseCase(pairing),
                            QrCodeGenerator(), DefaultStringProvider(compose.activity), DefaultDispatcherProvider())
                            .also { models.add(it) }
                    }
                })
            }
        }
    }

    private fun awaitText(id: Int) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun clickItem(index: Int, id: Int) {
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
        compose.onNode(hasText(text(id)) and hasClickAction()).performClick()
        compose.waitForIdle()
    }

    private fun awaitHome() {
        val header = hasText(text(R.string.alarm_settings_title)) and !hasClickAction()
        val settingsButton = hasText(text(R.string.watch_settings_title)) and hasClickAction()
        compose.waitUntil(15_000) {
            compose.onAllNodes(header or settingsButton)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Returning to the home destination can restore its previous bottom scroll position.
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNode(header).assertIsDisplayed()
    }

    private fun awaitQr() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription(text(R.string.onboarding_title))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithText(text(R.string.onboarding_refresh)).assertIsDisplayed()
    }

    @After fun stopModels() {
        models.forEach { it.viewModelScope.cancel() }
        if (::alarms.isInitialized) alarms.viewModelScope.cancel()
    }

    @Test fun pairedLaunchOpensAlarmsAndSettingsLinksOpenInfoAndQr() {
        pairing.paired = true
        start()
        awaitHome()
        assertEquals(0, pairing.refreshes)
        clickItem(4, R.string.watch_settings_title)
        awaitText(R.string.watch_pairing_info)
        clickItem(1, R.string.watch_pairing_info)
        awaitText(R.string.onboarding_paired_title)
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        awaitText(R.string.watch_pairing_info)
        clickItem(2, R.string.onboarding_generate_qr)
        awaitQr()
        assertEquals(1, pairing.refreshes)
        pairing.registered = true
        awaitHome()
        clickItem(4, R.string.watch_settings_title)
        awaitText(R.string.watch_pairing_info)
    }

    @Test fun firstPairingAutomaticallyOpensAlarmHome() {
        start()
        awaitQr()
        pairing.registered = true
        awaitHome()
        clickItem(4, R.string.watch_settings_title)
        awaitText(R.string.watch_pairing_info)
    }

    private class FakePairing : WatchPairingRepository {
        @Volatile var paired = false
        @Volatile var registered = false
        @Volatile var refreshes = 0
        override suspend fun getOrCreateDeviceUuid() = "test-watch"
        override suspend fun getFcmToken() = Result.success("test-token")
        override suspend fun refreshFcmToken(): Result<String> {
            refreshes++
            return Result.success("new-test-token")
        }
        override suspend fun isPaired() = paired
        override suspend fun setPaired(paired: Boolean) { this.paired = paired }
        override suspend fun checkFcmTokenRegistered(fcmToken: String) = NetworkResult.Success(registered)
    }
}
