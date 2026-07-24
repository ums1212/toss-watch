package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.comon.toss_watch.core.designsystem.theme.TossNumericStyle
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock

/**
 * 보유 종목 1행(카드) — 심볼 아바타, 종목명/코드/보유수량, 평가금액, 등락 Pill.
 *
 * @param onClick 카드 탭 — 해당 종목의 알림 추가 다이얼로그로 이동시키는 용도로 쓰인다.
 */
@Composable
fun HoldingListItem(
    holding: HoldingStock,
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
            SymbolAvatar(holding = holding)

            Spacer(modifier = Modifier.width(TossSpacing.stackSm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holding.stockName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${formatQuantity(holding.quantity)}주",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = holding.totalEvaluationAmount.formatAmount(holding.currency),
                    style = TossNumericStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ReturnRatePill(rate = holding.returnRate)
            }
        }
    }
}

/** 종목 코드를 보여주는 원형 아바타. */
@Composable
private fun SymbolAvatar(holding: HoldingStock) {
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
            text = holding.stockCode,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

/** 등락 방향에 따라 색상이 바뀌는 알약(pill) 배지 — 배경은 등락색 12% 틴트, 텍스트는 풀 컬러. */
@Composable
private fun ReturnRatePill(rate: Double) {
    val color = variationColor(rate)
    Text(
        text = formatRatePercent(rate),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(percent = 50))
            .padding(horizontal = TossSpacing.stackSm, vertical = 2.dp),
    )
}

private fun formatRatePercent(rate: Double): String {
    val sign = if (rate >= 0) "+" else ""
    return "$sign${"%.2f".format(rate)}%"
}

private fun formatQuantity(quantity: Double): String =
    if (quantity == quantity.toLong().toDouble()) {
        quantity.toLong().toString()
    } else {
        "%.4f".format(quantity).trimEnd('0').trimEnd('.')
    }

private fun Double.formatAmount(currency: Currency): String =
    when (currency) {
        Currency.KRW -> toKrw()
        Currency.USD -> toUsd()
    }

@Preview(showBackground = true)
@Composable
private fun HoldingListItemPreview() {
    TossWatchTheme {
        Column(verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm)) {
            HoldingListItem(
                holding = HoldingStock(
                    stockCode = "005930",
                    stockName = "삼성전자",
                    currency = Currency.KRW,
                    quantity = 10.0,
                    averageBuyPrice = 65_000.0,
                    totalBuyAmount = 650_000.0,
                    currentPrice = 72_500.0,
                    totalEvaluationAmount = 725_000.0,
                    profitLoss = 75_000.0,
                    returnRate = 11.54,
                ),
                onClick = {},
            )
            HoldingListItem(
                holding = HoldingStock(
                    stockCode = "SOXL",
                    stockName = "Direxion Semiconductor 3X",
                    currency = Currency.USD,
                    quantity = 50.0,
                    averageBuyPrice = 52.0,
                    totalBuyAmount = 2_600.0,
                    currentPrice = 68.0,
                    totalEvaluationAmount = 3_400.0,
                    profitLoss = 800.0,
                    returnRate = 30.77,
                ),
                onClick = {},
            )
            HoldingListItem(
                holding = HoldingStock(
                    stockCode = "TSLA",
                    stockName = "Tesla Inc",
                    currency = Currency.USD,
                    quantity = 5.0,
                    averageBuyPrice = 200.0,
                    totalBuyAmount = 1_000.0,
                    currentPrice = 175.40,
                    totalEvaluationAmount = 877.0,
                    profitLoss = -123.0,
                    returnRate = -12.30,
                ),
                onClick = {},
            )
        }
    }
}
