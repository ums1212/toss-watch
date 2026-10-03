package dev.comon.watch_app.presentation.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.component.WatchScrollColumn

/** 알람을 눌렀는데 시세 응답이 아직 오지 않은 상태. */
@Composable
fun StockQuoteLoadingScreen(onDismissClick: () -> Unit) {
    SwipeToDismissBox(onDismissed = onDismissClick) { isBackground ->
        if (isBackground) return@SwipeToDismissBox
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun StockQuoteErrorScreen(
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit,
    // null이 아니면 이어서 보여줄 종목이 남아 있다 — 이 종목 시세를 못 받아도 다음으로 넘어갈 수 있게 한다.
    onNextClick: (() -> Unit)? = null,
) {
    SwipeToDismissBox(onDismissed = onDismissClick) { isBackground ->
        if (isBackground) return@SwipeToDismissBox
        WatchScrollColumn {
            Text(
                text = stringResource(R.string.stock_alarm_quote_error),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetryClick,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.stock_alarm_retry))
            }
            if (onNextClick != null) {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Text(stringResource(R.string.stock_alarm_next))
                }
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
