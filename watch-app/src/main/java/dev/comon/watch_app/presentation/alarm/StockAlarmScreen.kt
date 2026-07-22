package dev.comon.watch_app.presentation.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.delay

private const val IMAGE_SCENE_DURATION_MS = 2000L

@Composable
fun StockAlarmScreen(
    stockName: String,
    currentPrice: String,
    changeRate: String,
    alarmVersion: Int,
    onDismissClick: () -> Unit,
) {
    val direction = remember(changeRate) { changeRate.toPriceDirection() }

    // 국내 시세 관례: 상승 빨강 / 하락 파랑 / 보합 중립.
    val badgeColor = when (direction) {
        PriceDirection.UP -> WatchColors.Red40
        PriceDirection.DOWN -> WatchColors.Blue40
        PriceDirection.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Scene 1(이미지) → Scene 2(종목 정보) 순서로 전환한다.
    // alarmVersion은 알람 화면이 떠 있는 동안 새 FCM이 도착할 때마다 증가하며(StockAlarmActivity),
    // 그때마다 이 LaunchedEffect가 재시작돼 이미지 Scene부터 다시 재생된다.
    var showInfoScene by remember { mutableStateOf(false) }
    // alarmVersion으로 remember 키를 걸어, 새 알람마다 false로 리셋된 상태가 반드시 한 프레임
    // 렌더링된 뒤 true로 바뀌도록 한다 — 그래야 슬라이드업 애니메이션이 매번 처음부터 재생된다.
    var imageVisible by remember(alarmVersion) { mutableStateOf(false) }

    LaunchedEffect(alarmVersion) {
        showInfoScene = false
        imageVisible = true
        delay(IMAGE_SCENE_DURATION_MS)
        showInfoScene = true
    }

    // Wear OS 표준 UX: 좌→우 스와이프로도 알람 화면을 닫을 수 있게 한다. (하단 닫기 버튼과 병행)
    SwipeToDismissBox(onDismissed = onDismissClick) { isBackground ->
        if (isBackground) return@SwipeToDismissBox

        Crossfade(targetState = showInfoScene, label = "stock_alarm_scene") { infoSceneVisible ->
            if (infoSceneVisible) {
                InfoScene(
                    stockName = stockName,
                    currentPrice = currentPrice,
                    changeRate = changeRate,
                    badgeColor = badgeColor,
                    onDismissClick = onDismissClick,
                )
            } else {
                ImageScene(direction = direction, visible = imageVisible)
            }
        }
    }
}

@Composable
private fun ImageScene(
    direction: PriceDirection,
    visible: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(direction.imageRes),
                contentDescription = stringResource(R.string.stock_alarm_image_desc),
                // 원형 워치 화면 특성상 Crop 시 가장자리가 많이 잘려 Fit으로 잘림 없이 표시한다.
                // fillMaxSize(0.8f)로 상하좌우 10%씩 여백을 준다.
                modifier = Modifier.fillMaxSize(0.8f),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun InfoScene(
    stockName: String,
    currentPrice: String,
    changeRate: String,
    badgeColor: Color,
    onDismissClick: () -> Unit,
) {
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
