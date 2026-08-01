package dev.comon.toss_watch.feature.alarm.presentation.component

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
import androidx.compose.ui.res.stringResource
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.alarm.R

/** 알림 요일 기본 선택값 — 월(0)~토(5). */
private val DEFAULT_SELECTED_DAYS = setOf(0, 1, 2, 3, 4, 5)

/**
 * 알림 추가 다이얼로그 — 종목 선택(드롭다운 또는 고정) + 시/분 입력 + 요일 선택.
 *
 * @param stocks 보유 종목 선택지. [lockedStock]이 지정되면 무시된다. 비어 있으면 안내 문구만 표시하고 '추가'를 비활성화한다.
 * @param initialStockCode 드롭다운 모드에서 미리 선택해 둘 종목 코드. `stocks`에서 일치하는 항목이 없으면 첫 번째 종목으로 대체된다.
 * @param lockedStock 지정되면 종목 드롭다운 대신 종목명을 정적으로 표시하고 이 종목으로 고정한다 —
 *   AlarmDetailScreen처럼 이미 종목 문맥이 정해진 화면에서 사용한다.
 * @param onConfirm (stockCode, stockName, hour, minute, daysOfWeek) 확정 콜백. `daysOfWeek`는 0(월)~6(일) 오름차순 정렬된 리스트.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(
    stocks: List<CachedStock>,
    onConfirm: (stockCode: String, stockName: String, hour: Int, minute: Int, daysOfWeek: List<Int>) -> Unit,
    onDismiss: () -> Unit,
    initialStockCode: String? = null,
    lockedStock: CachedStock? = null,
) {
    var selectedStock by remember {
        mutableStateOf(
            lockedStock ?: stocks.firstOrNull { it.stockCode == initialStockCode } ?: stocks.firstOrNull(),
        )
    }
    var isTickerMenuExpanded by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true,
    )
    var selectedDays by remember { mutableStateOf(DEFAULT_SELECTED_DAYS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.add_alarm_dialog_title)) },
        text = {
            Column {
                if (lockedStock == null && stocks.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.add_alarm_no_stocks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val stockLabel = stringResource(id = R.string.add_alarm_stock_label)

                    if (lockedStock != null) {
                        Text(
                            text = stockLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(TossSpacing.stackSm))
                        Text(
                            text = stringResource(
                                id = R.string.stock_display_format,
                                lockedStock.stockName,
                                lockedStock.stockCode,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = isTickerMenuExpanded,
                            onExpandedChange = { isTickerMenuExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedStock?.let {
                                    stringResource(id = R.string.stock_display_format, it.stockName, it.stockCode)
                                }.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stockLabel) },
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
                                        text = {
                                            Text(
                                                stringResource(
                                                    id = R.string.stock_display_format,
                                                    stock.stockName,
                                                    stock.stockCode,
                                                ),
                                            )
                                        },
                                        onClick = {
                                            selectedStock = stock
                                            isTickerMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(TossSpacing.containerMargin))

                    Text(text = stringResource(id = R.string.add_alarm_time_label))
                    Spacer(modifier = Modifier.height(TossSpacing.stackSm))
                    TimeInput(state = timePickerState)

                    Spacer(modifier = Modifier.height(TossSpacing.containerMargin))

                    Text(text = stringResource(id = R.string.add_alarm_days_label))
                    Spacer(modifier = Modifier.height(TossSpacing.stackSm))
                    DayOfWeekSelector(
                        selectedDays = selectedDays,
                        onToggleDay = { day ->
                            selectedDays = if (selectedDays.contains(day)) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedStock?.let { stock ->
                        onConfirm(
                            stock.stockCode,
                            stock.stockName,
                            timePickerState.hour,
                            timePickerState.minute,
                            selectedDays.sorted(),
                        )
                    }
                },
                enabled = selectedStock != null && selectedDays.isNotEmpty(),
            ) {
                Text(text = stringResource(id = R.string.add_alarm_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.add_alarm_cancel))
            }
        },
    )
}
