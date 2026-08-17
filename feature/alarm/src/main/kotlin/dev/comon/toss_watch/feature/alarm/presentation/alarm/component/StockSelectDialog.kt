package dev.comon.toss_watch.feature.alarm.presentation.alarm.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.alarm.R

/** 종목 항목 1건의 고정 높이 — 이 값 기준으로 최대 노출 개수만큼의 다이얼로그 높이를 계산한다. */
private val STOCK_ITEM_HEIGHT = 48.dp

/** 이 개수까지는 항목 수만큼 다이얼로그 높이가 늘어나고, 초과하면 이 높이로 고정되어 스크롤된다. */
private const val MAX_VISIBLE_STOCK_COUNT = 6

/**
 * 알림 탭에서 "알림 추가" 진입 시 뜨는 보유 종목 선택 다이얼로그.
 *
 * [AddAlarmDialog][dev.comon.toss_watch.feature.alarm.presentation.component.AddAlarmDialog]와
 * 달리 시각/요일 입력 없이 종목명만 나열한다 — 종목을 고르면 곧바로 해당 종목의
 * AlarmDetailScreen으로 이동시키는 용도이기 때문이다(확인 버튼 없이 항목 탭이 곧 확정).
 *
 * 종목 항목은 [STOCK_ITEM_HEIGHT]의 고정 높이를 가져, 목록이 [MAX_VISIBLE_STOCK_COUNT]개
 * 이하일 때는 항목 수만큼만 다이얼로그가 늘어나고(예: 1개면 1개 높이), 그 이상이면
 * [MAX_VISIBLE_STOCK_COUNT]개 높이로 고정되어 나머지는 스크롤로 노출된다.
 *
 * @param stocks 대시보드가 캐싱해 둔 보유 종목 목록. 비어 있으면 안내 문구만 표시한다.
 * @param onStockSelected 종목 항목 탭 시 호출.
 */
@Composable
fun StockSelectDialog(
    stocks: List<CachedStock>,
    onStockSelected: (CachedStock) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = stringResource(id = R.string.stock_select_dialog_title)) },
        text = {
            if (stocks.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.add_alarm_no_stocks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = STOCK_ITEM_HEIGHT * MAX_VISIBLE_STOCK_COUNT),
                ) {
                    items(items = stocks, key = { it.stockCode }) { stock ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(STOCK_ITEM_HEIGHT)
                                .clickable { onStockSelected(stock) },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = stock.stockName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = TossSpacing.stackSm),
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.add_alarm_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun StockSelectDialogPreview() {
    TossWatchTheme {
        StockSelectDialog(
            stocks = listOf(
                CachedStock(stockCode = "005930", stockName = "삼성전자"),
                CachedStock(stockCode = "035420", stockName = "NAVER"),
                CachedStock(stockCode = "000660", stockName = "SK하이닉스"),
            ),
            onStockSelected = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StockSelectDialogManyStocksPreview() {
    TossWatchTheme {
        StockSelectDialog(
            stocks = (1..12).map { CachedStock(stockCode = "%06d".format(it), stockName = "종목 $it") },
            onStockSelected = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StockSelectDialogEmptyPreview() {
    TossWatchTheme {
        StockSelectDialog(
            stocks = emptyList(),
            onStockSelected = {},
            onDismiss = {},
        )
    }
}
