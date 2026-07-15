package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.Blue40
import dev.comon.toss_watch.core.designsystem.theme.Red40
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.PortfolioSummary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** 포트폴리오 요약 카드 — 통화별 총 평가금액, 평가 손익, 계좌 전체 수익률. */
@Composable
fun PortfolioSummaryCard(
    portfolio: Portfolio?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "총 평가 자산",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val summary = portfolio?.summary
            Text(
                text = summary?.totalEvaluationKrw?.toKrw() ?: "—",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (summary != null && summary.hasUsdHoldings()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary.totalEvaluationUsd.toUsd(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (summary != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildProfitLabel(summary.totalProfitLossKrw, isUsd = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = variationColor(summary.totalProfitLossKrw),
                )
                if (summary.hasUsdHoldings()) {
                    Text(
                        text = buildProfitLabel(summary.totalProfitLossUsd, isUsd = true),
                        style = MaterialTheme.typography.bodyMedium,
                        color = variationColor(summary.totalProfitLossUsd),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildReturnRateLabel(summary.totalReturnRate),
                    style = MaterialTheme.typography.labelLarge,
                    color = variationColor(summary.totalReturnRate),
                )
            }
        }
    }
}

private fun PortfolioSummary.hasUsdHoldings(): Boolean =
    totalEvaluationUsd != 0.0 || totalInvestmentUsd != 0.0

private fun buildProfitLabel(amount: Double, isUsd: Boolean): String {
    val sign = if (amount >= 0) "+" else "-"
    val formatted = if (isUsd) abs(amount).toUsd() else abs(amount).toKrw()
    return "$sign$formatted"
}

private fun buildReturnRateLabel(rate: Double): String {
    val sign = if (rate >= 0) "+" else ""
    return "총 수익률 $sign${"%.2f".format(rate)}%"
}

/** 원화 금액 포맷 — 소수점은 반올림해 정수 단위로 표시한다. */
internal fun Double.toKrw(): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(this.roundToLong()) + "원"

/** 달러 금액 포맷 — 소수 2자리 고정. */
internal fun Double.toUsd(): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$${nf.format(this)}"
}

/** 국내 시세 관례 색상 — 상승 빨강 / 하락 파랑 / 보합 중립. */
@Composable
internal fun variationColor(value: Double): Color = when {
    value > 0 -> Red40
    value < 0 -> Blue40
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Preview(showBackground = true)
@Composable
private fun PortfolioSummaryCardPreview() {
    TossWatchTheme {
        PortfolioSummaryCard(
            portfolio = Portfolio(
                summary = PortfolioSummary(
                    totalInvestmentKrw = 650_000.0,
                    totalInvestmentUsd = 2_600_000.0,
                    totalEvaluationKrw = 725_000.0,
                    totalEvaluationUsd = 3_400_000.0,
                    totalProfitLossKrw = 75_000.0,
                    totalProfitLossUsd = 800_000.0,
                    totalReturnRate = 26.92,
                ),
                securities = emptyList(),
            ),
        )
    }
}
