package dev.comon.toss_watch.navigation.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/** [FloatingBottomNavigationBar] 탭 2개(대시보드/알림)의 인덱스. */
private const val TAB_DASHBOARD = 0
private const val TAB_ALARM = 1

/**
 * 바닥에서 띄워진 알약(pill) 형태의 플로팅 하단 네비게이션 바.
 *
 * [BottomMenuScreen]의 `Scaffold(bottomBar)` 슬롯에 배치되어 대시보드/알림 탭을 전환한다.
 * 선택 인디케이터는 라이트/다크 테마 어디서나 동일하게 보이도록 `primaryContainer`(토스 블루)
 * 배경 위에 검정 반투명 오버레이를 얹는 방식을 쓴다 — `colorScheme.primary`는 다크 테마에서
 * 밝은 라벤더색(`PrimaryDark`)이라 배경과 대비가 뒤집혀 버리기 때문이다.
 *
 * @param selectedIndex 현재 선택된 탭([TAB_DASHBOARD] 또는 [TAB_ALARM]).
 * @param onDashboardClick 대시보드 탭 클릭 시 호출.
 * @param onAlarmClick 알림 탭 클릭 시 호출.
 */
@Composable
fun FloatingBottomNavigationBar(
    selectedIndex: Int,
    onDashboardClick: () -> Unit,
    onAlarmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 12.dp, shape = pillShape)
                .background(color = MaterialTheme.colorScheme.primaryContainer, shape = pillShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FloatingNavItem(
                icon = Icons.Filled.Home,
                label = "대시보드",
                selected = selectedIndex == TAB_DASHBOARD,
                onClick = onDashboardClick,
                shape = pillShape,
            )
            FloatingNavItem(
                icon = Icons.Filled.Notifications,
                label = "알림",
                selected = selectedIndex == TAB_ALARM,
                onClick = onAlarmClick,
                shape = pillShape,
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.70f)

    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Color.Black.copy(alpha = 0.20f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(TossSpacing.stackSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingBottomNavigationBarPreview() {
    TossWatchTheme {
        FloatingBottomNavigationBar(
            selectedIndex = TAB_DASHBOARD,
            onDashboardClick = {},
            onAlarmClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingBottomNavigationBarAlarmSelectedPreview() {
    TossWatchTheme {
        FloatingBottomNavigationBar(
            selectedIndex = TAB_ALARM,
            onDashboardClick = {},
            onAlarmClick = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FloatingBottomNavigationBarDarkPreview() {
    TossWatchTheme(darkTheme = true) {
        FloatingBottomNavigationBar(
            selectedIndex = TAB_DASHBOARD,
            onDashboardClick = {},
            onAlarmClick = {},
        )
    }
}
