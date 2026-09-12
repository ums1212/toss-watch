package dev.comon.watch_app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.SwipeToDismissBox
import dev.comon.watch_app.presentation.alarmsettings.*
import dev.comon.watch_app.presentation.onboarding.OnboardingRoute
import java.time.LocalTime
import kotlinx.serialization.Serializable

@Serializable
sealed interface WatchRoute : NavKey {
    @Serializable data object Onboarding : WatchRoute
    @Serializable data object Settings : WatchRoute
    @Serializable data class Detail(val code: String, val name: String) : WatchRoute
    @Serializable data class Add(val code: String, val name: String) : WatchRoute
    @Serializable data object Time : WatchRoute
}

@Composable
fun WatchNavHost(viewModel: WatchAlarmViewModel = hiltViewModel()) {
    val backStack = rememberNavBackStack(WatchRoute.Onboarding)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val back: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            if (effect == WatchAlarmEffect.Submitted && backStack.lastOrNull() is WatchRoute.Add) back()
        }
    }
    SwipeToDismissBox(onDismissed = back, userSwipeEnabled = backStack.size > 1) { isBackground ->
    if (!isBackground) NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<WatchRoute.Onboarding> {
                OnboardingRoute(onAlarmSettingsClick = { backStack.add(WatchRoute.Settings) })
            }
            entry<WatchRoute.Settings> {
                LaunchedEffect(Unit) { viewModel.handleIntent(WatchAlarmIntent.Refresh) }
                WatchStockListScreen(state, viewModel::handleIntent, back) { stock ->
                    backStack.add(WatchRoute.Detail(stock.code, stock.name))
                }
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
}
