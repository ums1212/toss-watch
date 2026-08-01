package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.ui.graphics.vector.ImageVector
import dev.comon.toss_watch.feature.dashboard.R

/**
 * [PortfolioChartTypeSelector]/[PortfolioChartFullScreenDialog]/[PortfolioChartCard]에서
 * 전환 가능한 차트 종류. 새 차트 종류를 추가하려면 이 enum에 항목을 더하고, 각 캔버스
 * ([PortfolioBubbleChartCanvas], [PortfolioTreemapChartCanvas])와 소비처의 `when`을
 * 마저 채우면 된다 — [isAvailable]은 캔버스 구현이 아직 없는 항목을 선택기에는 노출하되
 * 비활성 상태로 자리만 잡아 두고 싶을 때 쓴다.
 *
 * [labelRes]는 문자열이 아니라 리소스 ID다 — 이 enum 자체는 Composable이 아니라
 * `stringResource()`를 호출할 수 없으므로, 실제 문자열 조회는 각 호출부(Composable)에서 한다.
 */
internal enum class PortfolioChartType(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val isAvailable: Boolean,
) {
    BUBBLE(labelRes = R.string.chart_type_bubble, icon = Icons.Filled.BubbleChart, isAvailable = true),
    TREEMAP(labelRes = R.string.chart_type_treemap, icon = Icons.Filled.GridView, isAvailable = true),
}

/** [PortfolioChartCard]/[PortfolioChartFullScreenDialog] 캡션에 쓰이는, 차트별 비중 표기 안내문 리소스 ID. */
internal val PortfolioChartType.captionRes: Int
    @StringRes get() = when (this) {
        PortfolioChartType.BUBBLE -> R.string.chart_weight_caption_bubble
        PortfolioChartType.TREEMAP -> R.string.chart_weight_caption_treemap
    }
