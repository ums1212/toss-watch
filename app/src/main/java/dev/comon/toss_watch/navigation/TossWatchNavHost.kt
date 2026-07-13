package dev.comon.toss_watch.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.comon.toss_watch.core.model.navigation.AuthRoute
import dev.comon.toss_watch.core.model.navigation.DashboardRoute
import dev.comon.toss_watch.core.model.navigation.SettingRoute
import dev.comon.toss_watch.feature.auth.presentation.login.LoginScreen
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.DashboardScreen
import dev.comon.toss_watch.feature.setting.presentation.setting.SettingScreen

/**
 * 최상위 Navigation 3 호스트.
 *
 * - 목적지는 :core:model의 @Serializable [AppRoute]만 사용 (문자열 라우팅 금지).
 * - :core:datastore의 세션 스트림([MainViewModel.sessionState])을 관찰해
 *   Splash → Auth/Dashboard 루트를 동적으로 전환한다.
 * - feature 화면은 SideEffect 콜백만 노출하므로 모듈 간 결합이 없다.
 */
@Composable
fun TossWatchNavHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            // 로그인 감지: 인증 플로우 위에 있을 때만 대시보드로 루트 교체
            // (구성 변경 후 재구독 시 Setting 백스택을 초기화하지 않도록 가드).
            SessionState.LOGGED_IN ->
                if (navigator.backStack.none { it is DashboardRoute }) {
                    navigator.setRoot(DashboardRoute)
                }

            // 로그아웃/세션 만료 감지: 보호된 화면을 모두 걷어내고 로그인으로.
            SessionState.LOGGED_OUT ->
                if (navigator.backStack.any { it !is AuthRoute }) {
                    navigator.setRoot(AuthRoute)
                }

            SessionState.LOADING -> Unit
        }
    }

    if (sessionState == SessionState.LOADING) {
        SplashScreen(modifier = modifier)
        return
    }

    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // 목적지별 ViewModelStore 분리 — 화면을 떠나면 해당 MVI ViewModel이 정리된다.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AuthRoute> {
                LoginScreen(
                    onNavigateToDashboard = { navigator.setRoot(DashboardRoute) },
                )
            }

            entry<DashboardRoute> {
                DashboardScreen(
                    onNavigateToSetting = { navigator.goTo(SettingRoute()) },
                )
            }

            entry<SettingRoute> { route ->
                SettingScreen(
                    watchToken = route.watchToken,
                    onNavigateBack = { navigator.goBack() },
                )
            }
        },
    )
}
