package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.component.WatchScrollColumn

// 페이저 한 장에 종목 하나만 크게 보여주고, 카드를 누르면 종목 알람 상세 화면으로 이동한다.
@Composable
internal fun WatchStockPageContent(page: WatchStockPage, onClick: () -> Unit) {
    // 화면 형태에 맞춘 여백과 세로 가운데 정렬은 WatchScrollColumn이 처리하고, 작은 화면에서 넘치면 세로로 스크롤된다.
    WatchScrollColumn {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            if (!page.held) {
                Text(
                    stringResource(R.string.alarm_other_stock_badge),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                page.stock.name,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.alarm_stock_count, page.alarmCount),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.alarm_stock_open),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
