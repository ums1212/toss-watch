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
import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** 잔고 요약 카드 — 총 자산, 평가 손익, 수익률. */
@Composable
fun AssetSummaryCard(
    userAssets: UserAssets?,
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

            Text(
                text = userAssets?.totalAssets?.toKrw() ?: "—",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (userAssets != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildProfitLabel(userAssets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = variationColor(userAssets.totalProfit.toDouble()),
                )
            }
        }
    }
}

private fun buildProfitLabel(userAssets: UserAssets): String {
    val sign = if (userAssets.totalProfit >= 0) "+" else "-"
    val amount = NumberFormat.getNumberInstance(Locale.KOREA)
        .format(abs(userAssets.totalProfit))
    return "$sign${amount}원 ($sign${"%.2f".format(abs(userAssets.profitRate))}%)"
}

internal fun Long.toKrw(): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(this) + "원"

/** 국내 시세 관례 색상 — 상승 빨강 / 하락 파랑 / 보합 중립. */
@Composable
internal fun variationColor(value: Double): Color = when {
    value > 0 -> Red40
    value < 0 -> Blue40
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Preview(showBackground = true)
@Composable
private fun AssetSummaryCardPreview() {
    TossWatchTheme {
        AssetSummaryCard(
            userAssets = UserAssets(
                totalAssets = 12_345_678L,
                totalProfit = 345_678L,
                profitRate = 2.88,
            ),
        )
    }
}
