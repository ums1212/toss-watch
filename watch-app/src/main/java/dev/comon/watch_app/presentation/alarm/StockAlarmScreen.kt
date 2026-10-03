package dev.comon.watch_app.presentation.alarm

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.component.WatchScrollColumn
import androidx.compose.ui.text.style.TextAlign
import dev.comon.watch_app.presentation.theme.TosswatchTheme
import dev.comon.watch_app.presentation.theme.WatchColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val IMAGE_SCENE_DURATION_MS = 2000L

@Composable
fun StockAlarmScreen(
    stockName: String,
    currentPrice: String,
    changeRate: String,
    alarmVersion: Int,
    onDismissClick: () -> Unit,
    // null이 아니면 이어서 보여줄 종목이 남아 있다 — 닫기 대신 “다음” 버튼을 보여준다.
    onNextClick: (() -> Unit)? = null,
    customImagePaths: Map<WatchAlarmImageSlot, String> = emptyMap(),
) {
    val direction = remember(changeRate) { changeRate.toPriceDirection() }

    // 국내 시세 관례: 상승 빨강 / 하락 파랑 / 보합 중립.
    val badgeColor = when (direction) {
        PriceDirection.UP -> WatchColors.Red40
        PriceDirection.DOWN -> WatchColors.Blue40
        PriceDirection.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Scene 1(이미지) → Scene 2(종목 정보) 순서로 전환한다.
    // alarmVersion은 알람 화면이 떠 있는 동안 새 알람이 울릴 때마다 증가하며(StockAlarmViewModel),
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
                    onNextClick = onNextClick,
                )
            } else {
                ImageScene(
                    direction = direction,
                    visible = imageVisible,
                    customImagePath = customImagePaths[direction.slot],
                )
            }
        }
    }
}

// 디버그 전용 알람 이미지 미리보기(WatchAlarmImagePreviewScreen)도 같은 모습으로 그리도록 공유한다.
@Composable
internal fun ImageScene(
    direction: PriceDirection,
    visible: Boolean,
    customImagePath: String? = null,
) {
    val defaultPainter = painterResource(direction.imageRes)
    // 사용자 지정 이미지는 메인 스레드 밖에서 디코딩한다. null = 아직 디코딩 중(슬라이드 인 초반 몇 ms).
    // 파일이 깨졌거나 읽지 못하면 기본 이미지로 대체한다.
    val painter by produceState<Painter?>(
        initialValue = if (customImagePath == null) defaultPainter else null,
        customImagePath,
        defaultPainter,
    ) {
        value = customImagePath
            ?.let { path -> withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) } }
            ?.let { BitmapPainter(it.asImageBitmap()) }
            ?: defaultPainter
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
            modifier = Modifier.fillMaxSize(),
        ) {
            painter?.let {
                Image(
                    painter = it,
                    contentDescription = stringResource(R.string.stock_alarm_image_desc),
                    // 이미지 영역은 이미지 종류와 무관하게 항상 화면 전체다 — 원형 디스플레이가 그대로
                    // 원형으로 잘라 보여준다. 이미지별 보정은 두지 않으며, 원 안에 어떻게 보일지는
                    // 이미지 쪽에서 원형 화면에 맞게 편집해 맞춘다(사용자 지정 이미지는 폰의 크롭 편집).
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

/**
 * 실제 워치 단말들(원형 소/대, 사각형)에서 [ImageScene]의 이미지가 화면을 꽉 채우는지,
 * 자막이 화면 밖으로 잘리지 않는지 한 번에 점검하기 위한 멀티 디바이스 프리뷰.
 */
@Preview(name = "Small Round", device = WearDevices.SMALL_ROUND, showBackground = true)
@Preview(name = "Large Round", device = WearDevices.LARGE_ROUND, showBackground = true)
@Preview(name = "Square", device = WearDevices.SQUARE, showBackground = true)
@Preview(name = "Rect", device = WearDevices.RECT, showBackground = true)
private annotation class StockAlarmImageDevicePreviews

@StockAlarmImageDevicePreviews
@Composable
private fun ImageSceneUpPreview() {
    TosswatchTheme {
        ImageScene(direction = PriceDirection.UP, visible = true)
    }
}

@StockAlarmImageDevicePreviews
@Composable
private fun ImageSceneFlatPreview() {
    TosswatchTheme {
        ImageScene(direction = PriceDirection.FLAT, visible = true)
    }
}

@StockAlarmImageDevicePreviews
@Composable
private fun ImageSceneDownPreview() {
    TosswatchTheme {
        ImageScene(direction = PriceDirection.DOWN, visible = true)
    }
}

@Composable
private fun InfoScene(
    stockName: String,
    currentPrice: String,
    changeRate: String,
    badgeColor: Color,
    onDismissClick: () -> Unit,
    onNextClick: (() -> Unit)?,
) {
    WatchScrollColumn {
        Text(
            text = stockName,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = stringResource(R.string.stock_alarm_current_price, currentPrice),
            textAlign = TextAlign.Center,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )

        Text(
            text = stringResource(R.string.stock_alarm_change_rate, changeRate),
            textAlign = TextAlign.Center,
            color = badgeColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 6.dp)
                .background(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )

        if (onNextClick != null) {
            Button(
                onClick = onNextClick,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.stock_alarm_next))
            }
        } else {
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
