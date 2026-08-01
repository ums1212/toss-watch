package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.R
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock

/**
 * 보유 종목 비중 차트(버블/트리맵)를 전체화면으로 보여주는 팝업.
 *
 * [DashboardScreen]에서 [PortfolioChartCard] 카드를 탭하면 뜬다. 차트 종류 전환은 이 팝업이
 * 아니라 대시보드의 "보유 종목" 섹션 헤더에 놓인 [PortfolioChartTypeSelector]가 담당하고,
 * 이 팝업은 [chartType]을 그대로 받아 보여주기만 한다 — 팝업을 열고 닫는 사이에도 선택된
 * 차트 종류가 유지되도록 상태를 [DashboardScreen] 쪽에 단일 소스로 둔 것이다.
 *
 * `AlertDialog` 대신 전체화면 폭이 필요해 raw [Dialog] + `usePlatformDefaultWidth = false`를
 * 쓴다 — 다른 다이얼로그([AccountSelectDialog], `TossWatchErrorDialog`)는 모두 `AlertDialog`
 * 슬롯 API를 쓰지만, 그걸로는 화면 폭을 가득 채울 수 없어 이 화면만 예외로 둔다.
 */
@Composable
internal fun PortfolioChartFullScreenDialog(
    holdings: List<HoldingStock>,
    chartType: PortfolioChartType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            PortfolioChartFullScreenContent(
                holdings = holdings,
                chartType = chartType,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun PortfolioChartFullScreenContent(
    holdings: List<HoldingStock>,
    chartType: PortfolioChartType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = TossSpacing.containerMargin),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TossSpacing.stackMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.chart_fullscreen_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.chart_fullscreen_close_desc),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = TossSpacing.stackMd),
            contentAlignment = Alignment.Center,
        ) {
            when (chartType) {
                PortfolioChartType.BUBBLE -> PortfolioBubbleChartCanvas(
                    holdings = holdings,
                    modifier = Modifier.fillMaxSize(),
                    maxBubbles = EXPANDED_MAX_CHART_ITEMS,
                    showWeightLabel = true,
                )

                PortfolioChartType.TREEMAP -> PortfolioTreemapChartCanvas(
                    holdings = holdings,
                    modifier = Modifier.fillMaxSize(),
                    maxItems = EXPANDED_MAX_CHART_ITEMS,
                    showWeightLabel = true,
                )
            }
        }

        Text(
            text = stringResource(id = chartType.captionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = TossSpacing.stackMd),
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PortfolioChartFullScreenContentPreview() {
    TossWatchTheme {
        Surface(
            modifier = Modifier.size(360.dp, 700.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            PortfolioChartFullScreenContent(
                holdings = sampleHoldings(),
                chartType = PortfolioChartType.BUBBLE,
                onDismiss = {},
            )
        }
    }
}
