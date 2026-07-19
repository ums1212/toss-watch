package dev.comon.toss_watch.feature.setting.presentation.setting.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.model.CachedStock

/**
 * 알림 추가 다이얼로그 — 보유 종목(대시보드 캐시) 드롭다운 + 시/분 입력.
 *
 * @param stocks 대시보드가 캐싱해 둔 보유 종목 선택지. 비어 있으면 안내 문구만 표시하고 '추가'를 비활성화한다.
 * @param onConfirm (stockCode, hour, minute) 확정 콜백 — [SettingUiIntent.OnAddAlarm]로 환원된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(
    stocks: List<CachedStock>,
    onConfirm: (stockCode: String, hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedStock by remember { mutableStateOf(stocks.firstOrNull()) }
    var isTickerMenuExpanded by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "알림 추가") },
        text = {
            Column {
                if (stocks.isEmpty()) {
                    Text(
                        text = "보유 종목이 없어요. 대시보드에서 계좌를 먼저 확인해 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = isTickerMenuExpanded,
                        onExpandedChange = { isTickerMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedStock?.let { "${it.stockName} (${it.stockCode})" }.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("종목") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = isTickerMenuExpanded,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = isTickerMenuExpanded,
                            onDismissRequest = { isTickerMenuExpanded = false },
                        ) {
                            stocks.forEach { stock ->
                                DropdownMenuItem(
                                    text = { Text("${stock.stockName} (${stock.stockCode})") },
                                    onClick = {
                                        selectedStock = stock
                                        isTickerMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "알림 시각")
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeInput(state = timePickerState)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedStock?.let { stock ->
                        onConfirm(stock.stockCode, timePickerState.hour, timePickerState.minute)
                    }
                },
                enabled = selectedStock != null,
            ) {
                Text(text = "추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        },
    )
}
