package dev.comon.watch_app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.TimePicker
import dev.comon.watch_app.presentation.alarmsettings.*
import dev.comon.watch_app.presentation.onboarding.OnboardingRoute
import dev.comon.watch_app.presentation.onboarding.WatchOnboardingViewModel
import dev.comon.watch_app.presentation.setting.WatchSettingsScreen
import java.time.LocalTime
import kotlinx.serialization.Serializable

@Serializable
sealed interface WatchRoute : NavKey {
    @Serializable data object Onboarding : WatchRoute
    @Serializable data object Settings : WatchRoute
    @Serializable data object AppSettings : WatchRoute
    @Serializable data object PairingInfo : WatchRoute
    @Serializable data object Qr : WatchRoute
    @Serializable data class Detail(val code: String, val name: String) : WatchRoute
    @Serializable data class Add(val code: String, val name: String) : WatchRoute
    @Serializable data object Time : WatchRoute
}

@Composable
fun WatchNavHost(
    viewModel: WatchAlarmViewModel = hiltViewModel(),
    pairingViewModel: @Composable () -> WatchOnboardingViewModel = { hiltViewModel() },
) {
    val backStack = rememberNavBackStack(WatchRoute.Onboarding)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val back: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    val showAlarms: () -> Unit = {
        backStack.clear()
        backStack.add(WatchRoute.Settings)
    }
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            if (effect == WatchAlarmEffect.Submitted && backStack.lastOrNull() is WatchRoute.Add) back()
        }
    }
    NavDisplay(
        backStack = backStack,
        onBack = back,
        sceneStrategies = listOf(remember { WatchSwipeSceneStrategy<NavKey>() }),
        // SwipeToDismissBox already animates the pop; avoid a second transition at release.
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<WatchRoute.Onboarding> {
                OnboardingRoute(onAlarmSettingsClick = showAlarms, viewModel = pairingViewModel(), onPaired = showAlarms)
            }
            entry<WatchRoute.Settings> {
                val foreground = LocalWatchForeground.current
                LaunchedEffect(foreground) {
                    if (foreground) viewModel.handleIntent(WatchAlarmIntent.Refresh)
                }
                WatchStockListScreen(state, viewModel::handleIntent, { backStack.add(WatchRoute.AppSettings) }) { stock ->
                    backStack.add(WatchRoute.Detail(stock.code, stock.name))
                }
            }
            entry<WatchRoute.AppSettings> {
                WatchSettingsScreen(
                    onPairingInfo = { backStack.add(WatchRoute.PairingInfo) },
                    onGenerateQr = { backStack.add(WatchRoute.Qr) },
                )
            }
            entry<WatchRoute.PairingInfo> {
                OnboardingRoute(
                    onAlarmSettingsClick = showAlarms,
                    viewModel = pairingViewModel(),
                    onGenerateQrClick = { backStack.add(WatchRoute.Qr) },
                )
            }
            entry<WatchRoute.Qr> {
                OnboardingRoute(onAlarmSettingsClick = showAlarms, viewModel = pairingViewModel(), onPaired = showAlarms, startWithQr = true)
            }
            entry<WatchRoute.Detail> { route ->
                WatchAlarmDetailScreen(route.code, route.name, state, viewModel::handleIntent, back) {
                    viewModel.handleIntent(WatchAlarmIntent.NewAlarm)
                    backStack.add(WatchRoute.Add(route.code, route.name))
                }
            }
            entry<WatchRoute.Add> { route ->
                WatchAddAlarmScreen(route.code, route.name, state, viewModel::handleIntent, back) {
                    backStack.add(WatchRoute.Time)
                }
            }
            entry<WatchRoute.Time> {
                TimePicker(initialTime = LocalTime.of(state.hour, state.minute), onTimePicked = {
                    viewModel.handleIntent(WatchAlarmIntent.SetTime(it.hour, it.minute))
                    back()
                })
            }
        },
    )
}
