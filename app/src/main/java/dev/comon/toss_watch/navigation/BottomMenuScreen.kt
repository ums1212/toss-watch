package dev.comon.toss_watch.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.feature.alarm.presentation.alarm.AlarmScreen
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.DashboardScreen
import dev.comon.toss_watch.navigation.component.FloatingBottomNavigationBar

/** [BottomMenuScreen] 하단 탭 — 왼쪽 대시보드, 오른쪽 알림. */
private enum class BottomTab {
    DASHBOARD,
    ALARM,
}

/** 바가 화면 밖으로 완전히 사라질 때 그림자까지 가리기 위한 여유값. */
private val HIDE_OVERSHOOT = 48.dp

/**
 * 대시보드/알림 화면을 하단 탭 2개로 감싸는 최상위 목적지.
 *
 * 탭 전환은 Navigation 3 백스택이 아닌 이 화면 내부의 로컬 상태로 처리된다 —
 * 대시보드/알림 각각의 ViewModel은 [dev.comon.toss_watch.navigation.Navigator]가 관리하는
 * BottomMenuRoute NavEntry의 ViewModelStore에 귀속되어, 탭을 오가도 상태가 유지된다.
 *
 * 하단 바는 바닥에서 띄워진 플로팅 알약 형태([FloatingBottomNavigationBar])로, 콘텐츠 위에
 * 겹쳐 그려진다. 리스트를 스크롤하는 동안에는(방향 무관) 바가 화면 아래로 슬라이드 아웃했다가
 * 스크롤이 멈추면 다시 슬라이드 인한다 — 단, 최상단에서 당겨서 새로고침하는 제스처는 제외된다.
 * 이를 위해 [NestedScrollConnection]을 각 탭 화면의 LazyColumn에 직접 전달해([DashboardScreen],
 * [AlarmScreen]) `PullToRefreshBox`보다 스크롤 이벤트를 먼저 관찰한다. 실제로 리스트가 스크롤된
 * 양(`consumed`)이 0이 아닐 때만 스크롤로 판단하는데, 풀투리프레시 당김은 리스트가 이미 최상단이라
 * 실제로는 전혀 스크롤되지 않고(consumed = 0) 남은 델타를 PullToRefreshBox가 가져가는 구조라서
 * 이 기준으로 자연히 걸러진다.
 *
 * @param onNavigateToSetting 설정 아이콘 탭 시 호출 — SettingRoute로 이동.
 * @param onNavigateToAlarmDetail 종목 항목(보유종목 카드 또는 알림 탭 항목) 탭 시 호출 —
 *   해당 종목의 AlarmDetailRoute로 이동한다. 상세 화면은 이 목적지 위에 전체화면으로 오버레이되므로
 *   하단 탭 바가 가려진다.
 */
@Composable
fun BottomMenuScreen(
    onNavigateToSetting: () -> Unit,
    onNavigateToAlarmDetail: (stockCode: String, stockName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.DASHBOARD) }

    // 스크롤 중일 때 true — true가 되는 순간 바가 슬라이드 아웃하고, 드래그/관성이 모두
    // 끝나면 다시 false로 돌아가 바가 슬라이드 인한다.
    var isScrolling by remember { mutableStateOf(false) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // consumed.y != 0 = 리스트가 실제로 스크롤되어 콘텐츠가 움직인 경우(방향 무관).
                // 최상단에서 더 당길 게 없어 리스트가 못 움직이는 풀투리프레시 제스처는 consumed가
                // 항상 0이므로(남은 available을 PullToRefreshBox가 당김 효과로 소비) 여기서 걸러진다.
                // 이 판단이 정확하려면 이 커넥션이 PullToRefreshBox보다 LazyColumn에 더 가깝게
                // 붙어 있어야 한다 — PullToRefreshBox가 먼저 소비해 버리면 구분할 수 없다.
                if (source == NestedScrollSource.UserInput && consumed.y != 0f) {
                    isScrolling = true
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // fling(관성 스크롤)은 손가락을 뗀 뒤에도 항상 디스패치되므로, 여기가 스크롤이
                // 완전히 멈춘 시점이다.
                isScrolling = false
                return Velocity.Zero
            }
        }
    }
    val slideProgress by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "floatingNavSlide",
    )
    val density = LocalDensity.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            FloatingBottomNavigationBar(
                selectedIndex = if (selectedTab == BottomTab.DASHBOARD) 0 else 1,
                onDashboardClick = { selectedTab = BottomTab.DASHBOARD },
                onAlarmClick = { selectedTab = BottomTab.ALARM },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = TossSpacing.stackMd)
                    .graphicsLayer {
                        translationY = slideProgress * (size.height + with(density) { HIDE_OVERSHOOT.toPx() })
                    },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                BottomTab.DASHBOARD ->
                    DashboardScreen(
                        onNavigateToSetting = onNavigateToSetting,
                        onNavigateToAlarmDetail = onNavigateToAlarmDetail,
                        bottomContentPadding = innerPadding.calculateBottomPadding(),
                        listNestedScrollConnection = nestedScrollConnection,
                    )

                BottomTab.ALARM ->
                    AlarmScreen(
                        onNavigateToAlarmDetail = onNavigateToAlarmDetail,
                        bottomContentPadding = innerPadding.calculateBottomPadding(),
                        listNestedScrollConnection = nestedScrollConnection,
                    )
            }
        }
    }
}
