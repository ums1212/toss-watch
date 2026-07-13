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

/** 표준 종목 선택지 — Phase 5에서 서버 종목 검색 API로 대체 예정. */
internal val StandardTickerOptions = listOf(
    "005930" to "삼성전자",
    "000660" to "SK하이닉스",
    "035420" to "NAVER",
    "035720" to "카카오",
    "005380" to "현대차",
)

/**
 * 알림 추가 다이얼로그 — 표준 종목 드롭다운 + 시/분 입력.
 *
 * @param onConfirm (stockCode, hour, minute) 확정 콜백 — [SettingUiIntent.OnAddAlarm]로 환원된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(
    onConfirm: (stockCode: String, hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTicker by remember { mutableStateOf(StandardTickerOptions.first()) }
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
                ExposedDropdownMenuBox(
                    expanded = isTickerMenuExpanded,
                    onExpandedChange = { isTickerMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = "${selectedTicker.second} (${selectedTicker.first})",
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
                        StandardTickerOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.second} (${option.first})") },
                                onClick = {
                                    selectedTicker = option
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        selectedTicker.first,
                        timePickerState.hour,
                        timePickerState.minute,
                    )
                },
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
