package dev.comon.watch_app.presentation.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.theme.WatchColors

@Composable
fun StockAlarmScreen(
    stockName: String,
    currentPrice: String,
    changeRate: String,
    onDismissClick: () -> Unit,
) {
    // 국내 시세 관례: 상승 빨강 / 하락 파랑 / 보합 중립.
    val badgeColor = when {
        changeRate.startsWith("-") -> WatchColors.Blue40
        changeRate.isNotBlank() && changeRate.trimStart('+') != "0%" -> WatchColors.Red40
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Wear OS 표준 UX: 좌→우 스와이프로도 알람 화면을 닫을 수 있게 한다. (하단 닫기 버튼과 병행)
    SwipeToDismissBox(onDismissed = onDismissClick) { isBackground ->
        if (isBackground) return@SwipeToDismissBox

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stockName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.stock_alarm_current_price, currentPrice),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )

            Text(
                text = stringResource(R.string.stock_alarm_change_rate, changeRate),
                color = badgeColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )

            Button(
                onClick = onDismissClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.stock_alarm_dismiss))
            }
        }
    }
}
