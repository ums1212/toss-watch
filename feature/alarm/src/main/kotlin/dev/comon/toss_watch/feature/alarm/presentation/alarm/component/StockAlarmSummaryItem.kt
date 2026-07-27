package dev.comon.toss_watch.feature.alarm.presentation.alarm.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.alarm.presentation.alarm.StockAlarmSummary

/** 알림이 등록된 종목 1행 — 심볼 아바타, 종목명/코드, 등록된 알림 개수. */
@Composable
fun StockAlarmSummaryItem(
    summary: StockAlarmSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TossSpacing.stackMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = summary.stockCode,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.width(TossSpacing.stackSm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.stockName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "알림 ${summary.alarmCount}개",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StockAlarmSummaryItemPreview() {
    TossWatchTheme {
        StockAlarmSummaryItem(
            summary = StockAlarmSummary(stockCode = "005930", stockName = "삼성전자", alarmCount = 3),
            onClick = {},
        )
    }
}
