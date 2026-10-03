package dev.comon.watch_app.presentation.alarm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.component.WatchScrollColumn
import dev.comon.watch_app.presentation.theme.TosswatchTheme

/** 알람 발화 직후 화면. 화면(또는 확인 버튼)을 누르면 시세 화면으로 넘어간다. */
@Composable
fun StockAlarmRingingScreen(
    stockName: String,
    onOpenClick: () -> Unit,
    onDismissClick: () -> Unit,
    // 이 종목 뒤에 이어서 보여줄 알람 수. 0보다 크면 “A 외 N개”로 안내한다.
    pendingCount: Int = 0,
) {
    SwipeToDismissBox(onDismissed = onDismissClick) { isBackground ->
        if (isBackground) return@SwipeToDismissBox
        WatchScrollColumn(modifier = Modifier.clickable(onClick = onOpenClick)) {
            // 한 줄 문장은 원형 화면 가장자리에서 양옆이 잘려, 종목 줄과 안내 줄로 나눈다.
            Text(
                text = if (pendingCount > 0) {
                    stringResource(R.string.stock_alarm_stock_and_more, stockName, pendingCount)
                } else {
                    stockName
                },
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.stock_alarm_arrived_message),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Button(
                    onClick = onDismissClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(stringResource(R.string.stock_alarm_dismiss))
                }
                Button(onClick = onOpenClick) {
                    Text(stringResource(R.string.stock_alarm_open))
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun StockAlarmRingingScreenPreview() {
    TosswatchTheme {
        StockAlarmRingingScreen(stockName = "삼성전자", onOpenClick = {}, onDismissClick = {}, pendingCount = 1)
    }
}
