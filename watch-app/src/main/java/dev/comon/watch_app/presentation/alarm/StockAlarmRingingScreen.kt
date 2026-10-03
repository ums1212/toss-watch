package dev.comon.watch_app.presentation.alarm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
            Text(
                text = if (pendingCount > 0) {
                    stringResource(R.string.stock_alarm_arrived_many, stockName, pendingCount)
                } else {
                    stringResource(R.string.stock_alarm_arrived, stockName)
                },
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onOpenClick,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.stock_alarm_open))
            }
            Button(
                onClick = onDismissClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(stringResource(R.string.stock_alarm_dismiss))
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun StockAlarmRingingScreenPreview() {
    TosswatchTheme {
        StockAlarmRingingScreen(stockName = "삼성전자", onOpenClick = {}, onDismissClick = {})
    }
}
