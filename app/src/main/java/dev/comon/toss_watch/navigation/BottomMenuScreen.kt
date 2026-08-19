package dev.comon.toss_watch.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.R
import dev.comon.toss_watch.core.designsystem.component.GuestModeBanner
import dev.comon.toss_watch.core.designsystem.theme.LocalTossWindowWidth
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWindowWidth
import dev.comon.toss_watch.feature.alarm.presentation.alarm.AlarmScreen
import dev.comon.toss_watch.feature.alarm.presentation.alarmdetail.AlarmDetailScreen
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.DashboardScreen
import dev.comon.toss_watch.navigation.component.FloatingBottomNavigationBar
import dev.comon.toss_watch.navigation.component.TossNavigationRail
import java.io.Serializable

/** [BottomMenuScreen] 하단 탭 — 왼쪽 대시보드, 오른쪽 알림. */
private enum class BottomTab {
    DASHBOARD,
    ALARM,
}

/** 바가 화면 밖으로 완전히 사라질 때 그림자까지 가리기 위한 여유값. */
private val HIDE_OVERSHOOT = 48.dp

/**
 * EXPANDED 목록+상세 2단 분할에서 상세 pane에 표시할 종목 키.
 * [rememberSaveable]의 기본 Saver가 Bundle에 담을 수 있어야 하므로 [Serializable]을 구현한다.
 */
private data class SelectedStock(val stockCode: String, val stockName: String?) : Serializable

/**
 * 대시보드/알림 화면을 하단 탭 2개로 감싸는 최상위 목적지.
 *
 * 탭 전환은 Navigation 3 백스택이 아닌 이 화면 내부의 로컬 상태로 처리된다 —
 * 대시보드/알림 각각의 ViewModel은 [dev.comon.toss_watch.navigation.Navigator]가 관리하는
 * BottomMenuRoute NavEntry의 ViewModelStore에 귀속되어, 탭을 오가도 상태가 유지된다.
 *
 * 내비게이션 UI는 [LocalTossWindowWidth]([TossWindowWidth])에 따라 세 가지 형태로 적응한다:
 * - COMPACT: 바닥에서 띄워진 플로팅 알약 형태([FloatingBottomNavigationBar])가 `Scaffold`의
 *   `bottomBar` 슬롯에서 콘텐츠 위에 겹쳐 그려진다. 리스트를 스크롤하는 동안에는(방향 무관) 바가
 *   화면 아래로 슬라이드 아웃했다가 스크롤이 멈추면 다시 슬라이드 인한다 — 단, 최상단에서 당겨서
 *   새로고침하는 제스처는 제외된다. 이를 위해 [NestedScrollConnection]을 각 탭 화면의 LazyColumn에
 *   직접 전달해([DashboardScreen], [AlarmScreen]) `PullToRefreshBox`보다 스크롤 이벤트를 먼저
 *   관찰한다. 실제로 리스트가 스크롤된 양(`consumed`)이 0이 아닐 때만 스크롤로 판단하는데,
 *   풀투리프레시 당김은 리스트가 이미 최상단이라 실제로는 전혀 스크롤되지 않고(consumed = 0)
 *   남은 델타를 PullToRefreshBox가 가져가는 구조라서 이 기준으로 자연히 걸러진다.
 * - MEDIUM/EXPANDED: 좁은/넓은 세로 레일([TossNavigationRail])이 탭 콘텐츠 왼쪽에 상시 노출된다.
 *   플로팅 바가 없으므로 스크롤에 따라 숨길 대상이 없어, `nestedScrollConnection`은 계속 연결되어
 *   있지만(코드 단순화를 위해 분기하지 않음) 시각적으로 아무 효과가 없다. 레일은 이 화면당 정확히
 *   한 번만 그려진다 — 아래 EXPANDED 목록+상세 분할이 레일까지 다시 감싸는 일은 없다.
 *
 * EXPANDED 폭에서는 탭 콘텐츠 영역 자체가 종목 선택 여부에 따라 단일/분할로 전환된다:
 * - 알림 탭은 [selectedStock]과 무관하게 항상 목록+상세 2단으로 분할된다(선택 전에는 상세 자리에
 *   안내 문구만 보인다).
 * - 대시보드 탭은 [selectedStock]이 없으면 목록만 전체 폭으로 보이고, 보유종목을 탭해
 *   [selectedStock]이 채워지는 순간 목록+상세로 분할된다. [selectedStock]은 탭을 오가도
 *   유지되므로 — 알림 탭에서 종목을 선택한 뒤 대시보드 탭으로 돌아와도 분할이 그대로 유지된다.
 *   이미 대시보드 탭에 있는 상태에서 하단 바/레일의 대시보드 아이콘을 한 번 더 누르면(재탭)
 *   [selectedStock]을 비워 단일 화면으로 되돌린다 — 다른 탭에 있다가 대시보드로 "전환"하는
 *   탭과, 대시보드에 이미 있는데 "재탭"하는 것을 구분해서 처리한다.
 * - 분할된 목록 pane은 전체 화면 폭 중 일부만 차지해 실제로는 좁으므로, 그 안의 [DashboardScreen]/
 *   [AlarmScreen]에는 [LocalTossWindowWidth]를 COMPACT로 오버라이드해 전달한다 — 그러지 않으면
 *   EXPANDED 전역 분류를 그대로 물려받아(2열 그리드, 넓은 여백 등) 좁은 폭에 눌려 텍스트가
 *   깨진다.
 * 이 분할은 [dev.comon.toss_watch.core.model.navigation.AlarmDetailRoute] push가 아니라
 * 이 화면 내부의 [selectedStock] 상태만으로 처리된다 — 새 AppRoute를 만들지 않는다(CLAUDE.md §4).
 * COMPACT/MEDIUM에서는 종목 탭 시 항상 [onNavigateToAlarmDetail]을 호출해 기존과 동일하게
 * 전체화면 오버레이로 이동한다.
 *
 * @param isGuest 게스트(더미 데이터 체험) 모드 여부 — true면 탭 콘텐츠 상단에 [GuestModeBanner]를 노출한다.
 * @param onNavigateToSetting 설정 아이콘 탭 시 호출 — SettingRoute로 이동.
 * @param onNavigateToAlarmDetail 종목 항목(보유종목 카드 또는 알림 탭 항목) 탭 시, COMPACT/MEDIUM에서
 *   호출되어 해당 종목의 AlarmDetailRoute로 이동한다(전체화면 오버레이). EXPANDED에서는 대신
 *   내부 [selectedStock] 상태로 처리되므로 호출되지 않는다.
 */
@Composable
fun BottomMenuScreen(
    isGuest: Boolean,
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
    val windowWidth = LocalTossWindowWidth.current
    val isCompact = windowWidth == TossWindowWidth.COMPACT
    val isExpanded = windowWidth == TossWindowWidth.EXPANDED

    // EXPANDED에서 종목을 선택하면 라우트를 push하는 대신 이 화면 내부 상태로 목록+상세 분할을 연다.
    var selectedStock by rememberSaveable { mutableStateOf<SelectedStock?>(null) }
    val onStockSelected: (String, String) -> Unit = { stockCode, stockName ->
        if (isExpanded) {
            selectedStock = SelectedStock(stockCode, stockName)
        } else {
            onNavigateToAlarmDetail(stockCode, stockName)
        }
    }

    // 상세 pane이 열려 있을 때 시스템 back은 그 pane을 닫는다 — 탭이 항상 분할 상태인 알림 탭이든,
    // 선택 시에만 분할되는 대시보드 탭이든 동일하게 선택 해제로 처리한다. 선택된 종목이 없을 때는
    // 이 핸들러가 비활성화되어 TossWatchNavHost의 바깥쪽 BackHandler(종료 다이얼로그)로 넘어간다.
    BackHandler(enabled = isExpanded && selectedStock != null) {
        selectedStock = null
    }

    // 다른 탭에 있다가 대시보드로 "전환"할 때는 selectedStock을 그대로 둬 분할 상태를 유지하지만,
    // 이미 대시보드 탭에 있는 상태에서 대시보드 아이콘을 다시 누르면(재탭) 분할을 접고 단일
    // 화면으로 되돌린다. 알림 탭은 선택과 무관하게 항상 분할이라 이런 재탭 처리가 필요 없다.
    val onDashboardTabClick = {
        if (selectedTab == BottomTab.DASHBOARD) {
            selectedStock = null
        }
        selectedTab = BottomTab.DASHBOARD
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // MEDIUM/EXPANDED에서는 TossNavigationRail이 탭 콘텐츠 옆에 상시 노출되므로
            // 이 슬롯은 비운다 — 플로팅 알약 바는 COMPACT 전용 UI다.
            if (isCompact) {
                FloatingBottomNavigationBar(
                    selectedIndex = if (selectedTab == BottomTab.DASHBOARD) 0 else 1,
                    onDashboardClick = onDashboardTabClick,
                    onAlarmClick = { selectedTab = BottomTab.ALARM },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = TossSpacing.stackMd)
                        .graphicsLayer {
                            translationY = slideProgress * (size.height + with(density) { HIDE_OVERSHOOT.toPx() })
                        },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (isGuest) {
                // 배너가 상태바 인셋을 직접 반영해 상태바 아래로 내려온다.
                GuestModeBanner(modifier = Modifier.statusBarsPadding())
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // 배너가 이미 상태바 인셋을 반영했으므로, 아래 레일/탭 화면의 TopAppBar가
                    // 같은 인셋을 또 반영해 배너와 타이틀 사이에 불필요한 여백이 생기지 않도록
                    // 이 서브트리 전체에서 상태바 인셋을 소비 처리한다. 게스트가 아닐 때는 배너가
                    // 없으므로 소비하지 않아야 레일/TopAppBar가 정상적으로 상태바를 피해 그려진다.
                    .let { if (isGuest) it.consumeWindowInsets(WindowInsets.statusBars) else it },
            ) {
                if (!isCompact) {
                    TossNavigationRail(
                        expanded = isExpanded,
                        selectedIndex = if (selectedTab == BottomTab.DASHBOARD) 0 else 1,
                        onDashboardClick = onDashboardTabClick,
                        onAlarmClick = { selectedTab = BottomTab.ALARM },
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // COMPACT는 플로팅 바가 콘텐츠 위에 겹쳐 그려지므로 innerPadding으로
                        // 하단 여백을 계산해 리스트에 넘긴다(bottomContentPadding). MEDIUM/EXPANDED는
                        // 겹쳐지는 바가 없는 대신 레일 옆 콘텐츠가 시스템 내비게이션 바에
                        // 직접 가릴 수 있어 여기서 navigationBarsPadding()으로 처리한다.
                        .let { if (isCompact) it else it.navigationBarsPadding() },
                ) {
                    // 알림 탭은 선택 여부와 무관하게 항상 분할되고, 대시보드 탭은 종목을 선택했을
                    // 때만 분할된다 — BottomMenuScreen 문서 참고.
                    val isSplit = isExpanded && (selectedTab == BottomTab.ALARM || selectedStock != null)

                    if (isSplit) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight(),
                            ) {
                                // 분할 모드의 목록 pane은 전체 화면의 일부만 차지해 실제로는 좁으므로,
                                // EXPANDED 전역 분류(2열 그리드, 넓은 여백)를 그대로 물려받으면 콘텐츠가
                                // 눌려 깨진다 — 이 서브트리에서만 COMPACT로 오버라이드해 폭에 맞는
                                // 레이아웃(1열, 좁은 여백)으로 그린다.
                                CompositionLocalProvider(LocalTossWindowWidth provides TossWindowWidth.COMPACT) {
                                    when (selectedTab) {
                                        BottomTab.DASHBOARD ->
                                            DashboardScreen(
                                                onNavigateToSetting = onNavigateToSetting,
                                                onNavigateToAlarmDetail = onStockSelected,
                                                listNestedScrollConnection = nestedScrollConnection,
                                            )

                                        BottomTab.ALARM ->
                                            AlarmScreen(
                                                onNavigateToAlarmDetail = onStockSelected,
                                                listNestedScrollConnection = nestedScrollConnection,
                                            )
                                    }
                                }
                            }

                            VerticalDivider()

                            Box(
                                modifier = Modifier
                                    .weight(0.58f)
                                    .fillMaxHeight(),
                            ) {
                                val stock = selectedStock
                                if (stock != null) {
                                    AlarmDetailScreen(
                                        stockCode = stock.stockCode,
                                        stockName = stock.stockName,
                                        onNavigateBack = { selectedStock = null },
                                        showBackButton = false,
                                    )
                                } else {
                                    AlarmDetailPanePlaceholder()
                                }
                            }
                        }
                    } else {
                        when (selectedTab) {
                            BottomTab.DASHBOARD ->
                                DashboardScreen(
                                    onNavigateToSetting = onNavigateToSetting,
                                    onNavigateToAlarmDetail = onStockSelected,
                                    bottomContentPadding = if (isCompact) innerPadding.calculateBottomPadding() else 0.dp,
                                    listNestedScrollConnection = nestedScrollConnection,
                                )

                            BottomTab.ALARM ->
                                AlarmScreen(
                                    onNavigateToAlarmDetail = onStockSelected,
                                    bottomContentPadding = if (isCompact) innerPadding.calculateBottomPadding() else 0.dp,
                                    listNestedScrollConnection = nestedScrollConnection,
                                )
                        }
                    }
                }
            }
        }
    }
}

/** EXPANDED 목록+상세 분할에서 아직 종목이 선택되지 않았을 때 상세 pane에 보이는 안내 문구. */
@Composable
private fun AlarmDetailPanePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(TossSpacing.sectionPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.alarm_detail_pane_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
