package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * [PortfolioChartTypeSelector]/[PortfolioChartFullScreenDialog]에서 전환 가능한 차트 종류.
 *
 * [TREEMAP]은 아직 캔버스 구현이 없어 [isAvailable]이 false다 — 선택기 아이콘에는 노출하되
 * 비활성 상태로 자리만 잡아 둔다. 추후 트리맵 캔버스를 구현하면 이 값만 true로 바꾸면 된다.
 */
internal enum class PortfolioChartType(
    val label: String,
    val icon: ImageVector,
    val isAvailable: Boolean,
) {
    BUBBLE(label = "버블", icon = Icons.Filled.BubbleChart, isAvailable = true),
    TREEMAP(label = "트리맵", icon = Icons.Filled.GridView, isAvailable = false),
}
