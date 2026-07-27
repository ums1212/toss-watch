package dev.comon.toss_watch.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.comon.toss_watch.core.model.navigation.AlarmDetailRoute
import dev.comon.toss_watch.core.model.navigation.AuthRoute
import dev.comon.toss_watch.core.model.navigation.BottomMenuRoute
import dev.comon.toss_watch.core.model.navigation.SettingRoute
import dev.comon.toss_watch.core.model.navigation.TossKeyRoute
import dev.comon.toss_watch.core.model.navigation.WatchPairRoute
import dev.comon.toss_watch.feature.alarm.presentation.alarmdetail.AlarmDetailScreen
import dev.comon.toss_watch.feature.auth.presentation.login.LoginScreen
import dev.comon.toss_watch.feature.setting.presentation.setting.SettingScreen
import dev.comon.toss_watch.feature.setting.presentation.watchpair.WatchPairScreen
import dev.comon.toss_watch.feature.tosskey.presentation.tosskey.TossKeyScreen

private val SLIDE_OVERLAY_ANIMATION_SPEC = tween<IntOffset>(durationMillis = 300)

/** [Context]를 감싸는 Wrapper 체인을 따라 올라가 실제 [Activity]를 찾는다. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 하위 화면 위에 겹쳐서 슬라이드 인/아웃하는 오버레이 전환 metadata.
 * 가려지는 화면(initial on 진입, target on pop)은 [ExitTransition.None]/[EnterTransition.None]으로
 * 애니메이션 없이 정적으로 유지되고, 위에 뜨는 화면만 좌우로 슬라이드된다.
 */
private fun slideOverlayTransitions(): Map<String, Any> =
    NavDisplay.transitionSpec {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = SLIDE_OVERLAY_ANIMATION_SPEC,
        ) togetherWith ExitTransition.None
    } + NavDisplay.popTransitionSpec {
        EnterTransition.None togetherWith
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = SLIDE_OVERLAY_ANIMATION_SPEC,
            )
    } + NavDisplay.predictivePopTransitionSpec { _ ->
        // 시스템 뒤로가기 제스처(predictive back)도 popTransitionSpec과 동일하게 동작.
        EnterTransition.None togetherWith
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = SLIDE_OVERLAY_ANIMATION_SPEC,
            )
    }

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
            // 로그인 감지: 인증 플로우 위에 있을 때만 하단 탭 화면으로 루트 교체
            // (구성 변경 후 재구독 시 Setting 백스택을 초기화하지 않도록 가드).
            SessionState.LOGGED_IN ->
                if (navigator.backStack.none { it is BottomMenuRoute }) {
                    navigator.setRoot(BottomMenuRoute)
                }

            // 세션은 있으나 토스 키 미등록: 온보딩 화면 위에 있을 때만 루트 교체
            // (설정 화면에서 재등록차 진입한 경우는 이미 LOGGED_IN이라 여기로 오지 않는다).
            SessionState.NEEDS_TOSS_KEY ->
                if (navigator.backStack.none { it is TossKeyRoute }) {
                    navigator.setRoot(TossKeyRoute)
                }

            // 로그아웃/세션 만료 감지: 보호된 화면을 모두 걷어내고 로그인으로.
            // (백스택이 비어 있는 콜드 스타트 최초 판별도 이 분기로 커버된다 —
            // `any { it !is AuthRoute }`는 빈 스택에서 false라 걸러지지 않는다.)
            SessionState.LOGGED_OUT ->
                if (navigator.backStack.singleOrNull() != AuthRoute) {
                    navigator.setRoot(AuthRoute)
                }

            SessionState.LOADING -> Unit
        }
    }

    // 백스택에 항목이 하나뿐인 최상단 화면(로그인/토스키 등록/메인 등 세션 상태별 루트)에서만 활성화 —
    // 그 외(백스택에 화면이 더 쌓인 상태)에는 Navigator.goBack()의 pop이 그대로 처리한다.
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val topRoute = navigator.backStack.singleOrNull()
    BackHandler(enabled = topRoute != null) {
        if (topRoute == TossKeyRoute) {
            // 토스키 등록은 로그인 완료를 전제로 한 온보딩 단계라, 뒤로가기는 앱 종료가 아니라
            // 등록을 취소하고 로그인 화면으로 되돌아가는 것으로 처리한다.
            navigator.setRoot(AuthRoute)
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        ExitAppDialog(
            onConfirm = {
                showExitDialog = false
                context.findActivity()?.finish()
            },
            onDismiss = { showExitDialog = false },
        )
    }

    // 세션 판별 전(LOADING)이거나, 판별은 끝났지만 위 LaunchedEffect가 아직
    // setRoot를 적용하지 못한 콜드 스타트 프레임(backStack 비어있음)에는
    // 낡은/빈 백스택이 그려지지 않도록 스플래시를 유지한다.
    if (sessionState == SessionState.LOADING || navigator.backStack.isEmpty()) {
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
                LoginScreen()
            }

            entry<BottomMenuRoute> {
                BottomMenuScreen(
                    onNavigateToSetting = { navigator.goTo(SettingRoute) },
                    onNavigateToAlarmDetail = { stockCode, stockName ->
                        navigator.goTo(AlarmDetailRoute(stockCode, stockName))
                    },
                )
            }

            entry<AlarmDetailRoute>(
                metadata = slideOverlayTransitions(),
            ) { route ->
                AlarmDetailScreen(
                    stockCode = route.stockCode,
                    stockName = route.stockName,
                    onNavigateBack = { navigator.goBack() },
                )
            }

            entry<SettingRoute>(
                metadata = slideOverlayTransitions(),
            ) {
                SettingScreen(
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToTossKey = { navigator.goTo(TossKeyRoute) },
                    onNavigateToWatchPair = { navigator.goTo(WatchPairRoute) },
                )
            }

            entry<TossKeyRoute>(
                metadata = slideOverlayTransitions(),
            ) {
                TossKeyScreen(
                    onNavigateBack = { navigator.goBack() },
                )
            }

            entry<WatchPairRoute>(
                metadata = slideOverlayTransitions(),
            ) {
                WatchPairScreen(
                    onNavigateBack = { navigator.goBack() },
                )
            }
        },
    )
}
