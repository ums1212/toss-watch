package dev.comon.toss_watch.navigation.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.R
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/** [TossNavigationRail] 탭 2개(대시보드/알림)의 인덱스. [FloatingBottomNavigationBar]와 동일한 규약. */
private const val TAB_DASHBOARD = 0
private const val TAB_ALARM = 1

private val RAIL_WIDTH = 88.dp
private val WIDE_RAIL_WIDTH = 220.dp

/**
 * MEDIUM/EXPANDED 창 폭에서 [FloatingBottomNavigationBar]를 대신하는 세로 네비게이션 레일.
 *
 * [FloatingBottomNavigationBar]와 같은 색 규약(`primaryContainer` 배경 + 검정 반투명 선택 오버레이 +
 * 흰 아이콘/라벨)을 세로 레이아웃으로 재현한다 — 두 컴포넌트가 다른 화면 크기에서 같은 브랜드
 * 아이덴티티로 보이도록 하기 위함이다.
 *
 * 이 컴포저블은 [dev.comon.toss_watch.navigation.BottomMenuScreen]의 `Scaffold` 바깥, 즉
 * Row의 일반 자식으로 배치되므로(=bottomBar 슬롯이 아니므로) 상태바/내비게이션바 인셋을
 * 스스로 처리하지 않는다 — 호출부가 필요에 따라 `.statusBarsPadding()`/`.navigationBarsPadding()`을
 * 붙인다(CLAUDE.md의 커스텀 bar 슬롯 인셋 규칙과 동일한 이유).
 *
 * @param expanded true면 EXPANDED 폭(아이콘+라벨 가로 배치의 Wide Rail), false면 MEDIUM 폭
 *   (아이콘 위/라벨 아래의 좁은 Rail)로 그려진다.
 * @param selectedIndex 현재 선택된 탭([TAB_DASHBOARD] 또는 [TAB_ALARM]).
 * @param onDashboardClick 대시보드 탭 클릭 시 호출.
 * @param onAlarmClick 알림 탭 클릭 시 호출.
 */
@Composable
fun TossNavigationRail(
    expanded: Boolean,
    selectedIndex: Int,
    onDashboardClick: () -> Unit,
    onAlarmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(if (expanded) WIDE_RAIL_WIDTH else RAIL_WIDTH)
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = TossSpacing.stackLg, horizontal = TossSpacing.stackSm),
        verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm),
    ) {
        RailNavItem(
            icon = Icons.Filled.Home,
            label = stringResource(id = R.string.bottom_nav_dashboard),
            selected = selectedIndex == TAB_DASHBOARD,
            onClick = onDashboardClick,
            expanded = expanded,
        )
        RailNavItem(
            icon = Icons.Filled.Notifications,
            label = stringResource(id = R.string.bottom_nav_alarm),
            selected = selectedIndex == TAB_ALARM,
            onClick = onAlarmClick,
            expanded = expanded,
        )
    }
}

@Composable
private fun RailNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.70f)
    val itemShape = RoundedCornerShape(16.dp)
    val itemModifier = modifier
        .fillMaxWidth()
        .clip(itemShape)
        .background(if (selected) Color.Black.copy(alpha = 0.20f) else Color.Transparent)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )

    if (expanded) {
        Row(
            modifier = itemModifier.padding(horizontal = TossSpacing.stackMd, vertical = TossSpacing.stackMd),
            horizontalArrangement = Arrangement.spacedBy(TossSpacing.stackMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    } else {
        Column(
            modifier = itemModifier.padding(vertical = TossSpacing.stackMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm / 2),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}

@Preview(name = "Medium Rail", showBackground = true, heightDp = 640)
@Composable
private fun TossNavigationRailMediumPreview() {
    TossWatchTheme {
        TossNavigationRail(
            expanded = false,
            selectedIndex = TAB_DASHBOARD,
            onDashboardClick = {},
            onAlarmClick = {},
        )
    }
}

@Preview(name = "Wide Rail (Expanded)", showBackground = true, heightDp = 640)
@Composable
private fun TossNavigationRailExpandedPreview() {
    TossWatchTheme {
        TossNavigationRail(
            expanded = true,
            selectedIndex = TAB_ALARM,
            onDashboardClick = {},
            onAlarmClick = {},
        )
    }
}

@Preview(name = "Wide Rail Dark", showBackground = true, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TossNavigationRailExpandedDarkPreview() {
    TossWatchTheme(darkTheme = true) {
        TossNavigationRail(
            expanded = true,
            selectedIndex = TAB_DASHBOARD,
            onDashboardClick = {},
            onAlarmClick = {},
        )
    }
}
