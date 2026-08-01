package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.designsystem.theme.toss
import dev.comon.toss_watch.feature.dashboard.R
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.PortfolioSummary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * 계좌 요약 카드 — 브랜드 블루 그라디언트 배경에 총 평가자산·수익률 배지·
 * 평가손익/수익률 2분할 지표를 담는다.
 *
 * 카드 배경이 [MaterialTheme.toss.fall](파랑)과 같은 계열이라 카드 내부의
 * 손익/수익률은 등락색 대신 온컬러(흰색 계열) 톤으로 고정한다. 등락 컬러
 * 코딩은 버블 차트·보유종목 리스트에서만 사용한다.
 */
@Composable
fun PortfolioSummaryCard(
    portfolio: Portfolio?,
    accountNo: String?,
    modifier: Modifier = Modifier,
) {
    val summary = portfolio?.summary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary,
                    ),
                ),
            )
            .padding(TossSpacing.stackLg),
    ) {
        if (accountNo != null) {
            Text(
                text = stringResource(id = R.string.portfolio_summary_account_suffix, accountNo),
                style = MaterialTheme.typography.labelMedium,
                color = onPrimary.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = stringResource(id = R.string.portfolio_summary_total_valuation),
            style = MaterialTheme.typography.labelLarge,
            color = onPrimary.copy(alpha = 0.8f),
        )

        Spacer(modifier = Modifier.height(TossSpacing.stackSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary?.totalEvaluationKrw?.toKrw() ?: "—",
                style = MaterialTheme.typography.displaySmall,
                color = onPrimary,
            )

            if (summary != null) {
                ReturnRateBadge(rate = summary.totalReturnRate, onPrimary = onPrimary)
            }
        }

        if (summary != null && summary.hasUsdHoldings()) {
            Spacer(modifier = Modifier.height(TossSpacing.stackSm))
            Text(
                text = summary.totalEvaluationUsd.toUsd(),
                style = MaterialTheme.typography.bodyMedium,
                color = onPrimary.copy(alpha = 0.7f),
            )
        }

        if (summary != null) {
            Spacer(modifier = Modifier.height(TossSpacing.stackMd))
            HorizontalDivider(color = onPrimary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(TossSpacing.stackMd))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.portfolio_summary_profit_loss),
                    valueKrw = buildProfitLabel(summary.totalProfitLossKrw, isUsd = false),
                    valueUsd = if (summary.hasUsdHoldings()) {
                        buildProfitLabel(summary.totalProfitLossUsd, isUsd = true)
                    } else {
                        null
                    },
                    isPositive = summary.totalProfitLossKrw >= 0,
                    onPrimary = onPrimary,
                )
                SummaryStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(id = R.string.portfolio_summary_total_return),
                    valueKrw = formatRatePercent(summary.totalReturnRate),
                    valueUsd = null,
                    isPositive = summary.totalReturnRate >= 0,
                    onPrimary = onPrimary,
                )
            }
        }
    }
}

@Composable
private fun ReturnRateBadge(rate: Double, onPrimary: Color) {
    Text(
        text = formatRatePercent(rate),
        style = MaterialTheme.typography.labelLarge,
        color = onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(onPrimary.copy(alpha = 0.18f))
            .padding(horizontal = TossSpacing.stackSm, vertical = 4.dp),
    )
}

@Composable
private fun SummaryStat(
    label: String,
    valueKrw: String,
    valueUsd: String?,
    isPositive: Boolean,
    onPrimary: Color,
    modifier: Modifier = Modifier,
) {
    // 파란 배경 위에서는 하락(파랑) 등락색이 배경과 섞여 보이지 않으므로
    // 부호는 텍스트(+/-)로만 구분하고 색상은 온컬러 톤 하나로 고정한다.
    val valueColor = if (isPositive) onPrimary else onPrimary.copy(alpha = 0.75f)
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = onPrimary.copy(alpha = 0.8f),
        )
        Text(
            text = valueKrw,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
        if (valueUsd != null) {
            Text(
                text = valueUsd,
                style = MaterialTheme.typography.bodySmall,
                color = onPrimary.copy(alpha = 0.7f),
            )
        }
    }
}

private fun PortfolioSummary.hasUsdHoldings(): Boolean =
    totalEvaluationUsd != 0.0 || totalInvestmentUsd != 0.0

@Composable
private fun buildProfitLabel(amount: Double, isUsd: Boolean): String {
    val sign = if (amount >= 0) "+" else "-"
    val formatted = if (isUsd) abs(amount).toUsd() else abs(amount).toKrw()
    return "$sign$formatted"
}

private fun formatRatePercent(rate: Double): String {
    val sign = if (rate >= 0) "+" else ""
    return "$sign${"%.2f".format(rate)}%"
}

/** 원화 금액 포맷 — 소수점은 반올림해 정수 단위로 표시한다. */
@Composable
internal fun Double.toKrw(): String =
    stringResource(
        id = R.string.dashboard_krw_amount_format,
        NumberFormat.getNumberInstance(Locale.KOREA).format(this.roundToLong()),
    )

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
    value > 0 -> MaterialTheme.toss.rise
    value < 0 -> MaterialTheme.toss.fall
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
            accountNo = "100012345678",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioSummaryCardKrwOnlyNegativePreview() {
    TossWatchTheme {
        PortfolioSummaryCard(
            portfolio = Portfolio(
                summary = PortfolioSummary(
                    totalInvestmentKrw = 800_000.0,
                    totalInvestmentUsd = 0.0,
                    totalEvaluationKrw = 725_000.0,
                    totalEvaluationUsd = 0.0,
                    totalProfitLossKrw = -75_000.0,
                    totalProfitLossUsd = 0.0,
                    totalReturnRate = -9.38,
                ),
                securities = emptyList(),
            ),
            accountNo = "100098765432",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PortfolioSummaryCardEmptyPreview() {
    TossWatchTheme {
        PortfolioSummaryCard(portfolio = null, accountNo = null)
    }
}
