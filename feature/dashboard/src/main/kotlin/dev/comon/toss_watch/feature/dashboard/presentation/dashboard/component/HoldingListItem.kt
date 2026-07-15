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
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock
import kotlin.math.abs

/** 보유 종목 1행 — 종목명/코드/보유수량, 현재가·평가금액, 평가손익·수익률 컬러 코딩. */
@Composable
fun HoldingListItem(
    holding: HoldingStock,
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
                text = holding.stockName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${holding.stockCode} · ${formatQuantity(holding.quantity)}주",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = holding.totalEvaluationAmount.formatAmount(holding.currency),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatProfitAndRate(holding),
                style = MaterialTheme.typography.labelLarge,
                color = variationColor(holding.profitLoss),
            )
        }
    }
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

private fun formatProfitAndRate(holding: HoldingStock): String {
    val sign = if (holding.profitLoss >= 0) "+" else "-"
    val rateSign = if (holding.returnRate >= 0) "+" else ""
    return "$sign${abs(holding.profitLoss).formatAmount(holding.currency)} ($rateSign${"%.2f".format(holding.returnRate)}%)"
}

@Preview(showBackground = true)
@Composable
private fun HoldingListItemPreview() {
    TossWatchTheme {
        Column {
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
            )
        }
    }
}
