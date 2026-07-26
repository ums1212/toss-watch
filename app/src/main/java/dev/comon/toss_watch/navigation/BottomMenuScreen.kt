package dev.comon.toss_watch.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.comon.toss_watch.feature.alarm.presentation.alarm.AlarmScreen
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.DashboardScreen

/** [BottomMenuScreen] 하단 탭 — 왼쪽 대시보드, 오른쪽 알림. */
private enum class BottomTab {
    DASHBOARD,
    ALARM,
}

/**
 * 대시보드/알림 화면을 하단 탭 2개로 감싸는 최상위 목적지.
 *
 * 탭 전환은 Navigation 3 백스택이 아닌 이 화면 내부의 로컬 상태로 처리된다 —
 * 대시보드/알림 각각의 ViewModel은 [dev.comon.toss_watch.navigation.Navigator]가 관리하는
 * BottomMenuRoute NavEntry의 ViewModelStore에 귀속되어, 탭을 오가도 상태가 유지된다.
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.DASHBOARD,
                    onClick = { selectedTab = BottomTab.DASHBOARD },
                    icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "대시보드") },
                    label = { Text(text = "대시보드") },
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.ALARM,
                    onClick = { selectedTab = BottomTab.ALARM },
                    icon = { Icon(imageVector = Icons.Filled.Notifications, contentDescription = "알림") },
                    label = { Text(text = "알림") },
                )
            }
        },
    ) { innerPadding ->
        // 하단 탭 바가 차지하는 높이만 예약한다 — 상단은 각 탭 화면이 자체 TopAppBar로 처리한다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            when (selectedTab) {
                BottomTab.DASHBOARD ->
                    DashboardScreen(
                        onNavigateToSetting = onNavigateToSetting,
                        onNavigateToAlarmDetail = onNavigateToAlarmDetail,
                    )

                BottomTab.ALARM ->
                    AlarmScreen(
                        onNavigateToAlarmDetail = onNavigateToAlarmDetail,
                    )
            }
        }
    }
}
