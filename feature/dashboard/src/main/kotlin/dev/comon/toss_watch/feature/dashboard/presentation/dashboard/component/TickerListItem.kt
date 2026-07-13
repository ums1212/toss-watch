package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker

/** 관찰 종목 1행 — 종목명/코드, 현재가, 등락률 컬러 코딩. */
@Composable
fun TickerListItem(
    ticker: TargetTicker,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticker.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = ticker.code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = ticker.currentPrice.toKrw(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatChangeRate(ticker.changeRate),
                style = MaterialTheme.typography.labelLarge,
                color = variationColor(ticker.changeRate),
            )
        }
    }
}

private fun formatChangeRate(changeRate: Double): String {
    val sign = if (changeRate >= 0) "+" else ""
    return "$sign${"%.2f".format(changeRate)}%"
}

@Preview(showBackground = true)
@Composable
private fun TickerListItemPreview() {
    TossWatchTheme {
        Column {
            TickerListItem(
                ticker = TargetTicker("005930", "삼성전자", 78_400L, 1.42),
            )
            TickerListItem(
                ticker = TargetTicker("035420", "NAVER", 187_500L, -0.83),
            )
        }
    }
}
