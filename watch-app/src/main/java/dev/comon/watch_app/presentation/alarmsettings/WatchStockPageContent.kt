package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

// 원형 카드의 지름 = 페이지 가로폭 대비 비율.
private const val CircleCardWidthFraction = 0.8f

// 원에 내접하는 정사각형은 지름의 약 70.7%이므로, 글자가 원 가장자리에 잘리지 않도록 그 안쪽에만 배치한다.
private const val CircleContentFraction = 0.72f

// 페이저 한 장에 종목 하나만 원형 카드로 크게 보여주고, 카드를 누르면 종목 알람 상세 화면으로 이동한다.
@Composable
internal fun WatchStockPageContent(page: WatchStockPage, onClick: () -> Unit) {
    // 화면 형태에 맞춘 여백과 세로 가운데 정렬은 WatchScrollColumn이 처리하고, 작은 화면에서 넘치면 세로로 스크롤된다.
    WatchScrollColumn {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(CircleCardWidthFraction).aspectRatio(1f),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxSize(CircleContentFraction),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (!page.held) {
                        Text(
                            stringResource(R.string.alarm_other_stock_badge),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        page.stock.name,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.alarm_stock_count, page.alarmCount),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
