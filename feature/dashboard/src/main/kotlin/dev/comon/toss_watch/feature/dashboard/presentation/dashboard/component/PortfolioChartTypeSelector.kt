package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/**
 * [DashboardScreen]의 "보유 종목" 섹션 헤더 오른쪽에 놓이는, 차트 종류(버블/트리맵)를
 * 아이콘만으로 전환하는 알약(pill) 토글.
 *
 * `app` 모듈의 `FloatingBottomNavigationBar`와 동일한 시각 언어(알약 모양 + shadow +
 * `primaryContainer` 배경 + 선택 시 검정 반투명 오버레이)를 재사용한다 — 다만 그 컴포넌트는
 * `app` 모듈 전용이라(`:feature:dashboard`가 `:app`을 의존할 수 없음) 직접 재사용할 수 없어
 * 같은 디자인을 이 모듈 안에 더 작은 인라인 크기로 다시 구현했다.
 */
@Composable
internal fun PortfolioChartTypeSelector(
    selected: PortfolioChartType,
    onSelect: (PortfolioChartType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = pillShape)
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = pillShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PortfolioChartType.entries.forEach { type ->
            PortfolioChartTypeItem(
                type = type,
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = pillShape,
            )
        }
    }
}

@Composable
private fun PortfolioChartTypeItem(
    type: PortfolioChartType,
    selected: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = when {
        !type.isAvailable -> Color.White.copy(alpha = 0.35f)
        selected -> Color.White
        else -> Color.White.copy(alpha = 0.70f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Color.Black.copy(alpha = 0.20f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = type.isAvailable,
                onClick = onClick,
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = type.label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioChartTypeSelectorPreview() {
    TossWatchTheme {
        PortfolioChartTypeSelector(
            selected = PortfolioChartType.BUBBLE,
            onSelect = {},
        )
    }
}
