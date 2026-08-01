package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock

/**
 * 보유 종목의 평가금액 비중을 보여주는 차트 카드("Market Performance").
 *
 * [chartType]에 따라 버블/트리맵 캔버스를 전환해 그린다 — [DashboardScreen]의 "보유 종목"
 * 섹션 헤더에 놓인 [PortfolioChartTypeSelector]가 이 값을 결정하며, 카드를 탭했을 때 열리는
 * 전체화면 팝업([PortfolioChartFullScreenDialog])도 같은 [chartType]을 받아 미리보기와
 * 항상 같은 차트 종류를 보여준다.
 *
 * 카드 전체가 탭 가능하며, 탭하면 [onClick]을 통해 더 많은 종목을 더 자세히 볼 수 있는
 * 전체화면 팝업으로 확대하는 진입점 역할만 한다 — 실제 그리기 로직은
 * [PortfolioBubbleChartCanvas]/[PortfolioTreemapChartCanvas]가 카드/전체화면 뷰 사이에서 공유된다.
 */
@Composable
internal fun PortfolioChartCard(
    holdings: List<HoldingStock>,
    chartType: PortfolioChartType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (holdings.isEmpty()) return

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(TossSpacing.stackMd)) {
            val canvasModifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)

            when (chartType) {
                PortfolioChartType.BUBBLE -> PortfolioBubbleChartCanvas(
                    holdings = holdings,
                    modifier = canvasModifier,
                )

                PortfolioChartType.TREEMAP -> PortfolioTreemapChartCanvas(
                    holdings = holdings,
                    modifier = canvasModifier,
                )
            }

            Text(
                text = stringResource(id = chartType.captionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = TossSpacing.stackSm),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioChartCardBubblePreview() {
    TossWatchTheme {
        PortfolioChartCard(
            holdings = sampleHoldings(),
            chartType = PortfolioChartType.BUBBLE,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioChartCardTreemapPreview() {
    TossWatchTheme {
        PortfolioChartCard(
            holdings = sampleHoldings(),
            chartType = PortfolioChartType.TREEMAP,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioChartCardSingleHoldingPreview() {
    TossWatchTheme {
        PortfolioChartCard(
            holdings = sampleSingleHolding(),
            chartType = PortfolioChartType.BUBBLE,
            onClick = {},
        )
    }
}
