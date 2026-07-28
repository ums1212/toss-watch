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
import androidx.compose.ui.tooling.preview.Preview
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock

/**
 * 보유 종목의 평가금액 비중을 원 크기로 보여주는 버블 차트 카드("Market Performance").
 *
 * 카드 전체가 탭 가능하며, 탭하면 [onClick]을 통해 더 많은 종목을 더 자세히 볼 수 있는
 * 전체화면 팝업([PortfolioChartFullScreenDialog])으로 확대하는 진입점 역할만 한다 —
 * 실제 그리기 로직은 [PortfolioBubbleChartCanvas]가 카드/전체화면 뷰 사이에서 공유된다.
 */
@Composable
fun PortfolioBubbleChart(
    holdings: List<HoldingStock>,
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
            PortfolioBubbleChartCanvas(
                holdings = holdings,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f),
            )

            Text(
                text = "원 크기는 포트폴리오 내 평가금액 비중을 나타내요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = TossSpacing.stackSm),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioBubbleChartPreview() {
    TossWatchTheme {
        PortfolioBubbleChart(
            holdings = sampleHoldings(),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioBubbleChartSingleHoldingPreview() {
    TossWatchTheme {
        PortfolioBubbleChart(
            holdings = sampleSingleHolding(),
            onClick = {},
        )
    }
}
