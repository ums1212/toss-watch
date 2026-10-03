package dev.comon.watch_app.presentation.setting

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import dev.comon.toss_watch.core.model.watch.WatchAlarmImageSlot
import dev.comon.watch_app.R
import dev.comon.watch_app.presentation.alarm.ImageScene
import dev.comon.watch_app.presentation.alarm.PriceDirection
import dev.comon.watch_app.presentation.navigation.LocalWatchForeground
import dev.comon.watch_app.presentation.theme.TosswatchTheme

// 페이저 순서: 상승 → 보합 → 하락.
private val PREVIEW_PAGES = listOf(PriceDirection.UP, PriceDirection.FLAT, PriceDirection.DOWN)

/** 디버그 빌드 전용 — 설정된 알람 이미지 3장을 실제 알람과 같은 모습으로 한 장씩 넘겨 본다. */
@Composable
fun WatchAlarmImagePreviewScreen(viewModel: WatchAlarmImagePreviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 폰에서 이미지를 바꾼 뒤 이 화면으로 돌아오면 다시 읽는다.
    val foreground = LocalWatchForeground.current
    LaunchedEffect(foreground) {
        if (foreground) viewModel.handleIntent(WatchAlarmImagePreviewIntent.Reload)
    }
    WatchAlarmImagePreviewContent(state.customImagePaths)
}

@Composable
private fun WatchAlarmImagePreviewContent(customImagePaths: Map<WatchAlarmImageSlot, String>) {
    val pagerState = rememberPagerState { PREVIEW_PAGES.size }

    AppScaffold {
        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(
                state = pagerState,
                // HorizontalPager는 기본적으로 베젤 입력을 받지 않으므로, 한 칸에 한 장씩 넘기도록 켠다.
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(pagerState),
                key = { page -> PREVIEW_PAGES[page] },
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    val direction = PREVIEW_PAGES[page]
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 알람 화면과 같은 컴포저블로 그려, 여기서 본 모습이 곧 실제 알람의 모습이 되게 한다.
                        ImageScene(
                            direction = direction,
                            visible = true,
                            customImagePath = customImagePaths[direction.slot],
                        )
                        Text(
                            text = stringResource(direction.labelRes),
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@get:StringRes
private val PriceDirection.labelRes: Int
    get() = when (this) {
        PriceDirection.UP -> R.string.alarm_image_preview_up
        PriceDirection.FLAT -> R.string.alarm_image_preview_flat
        PriceDirection.DOWN -> R.string.alarm_image_preview_down
    }

@Preview(device = WearDevices.SMALL_ROUND, showBackground = true)
@Composable
private fun WatchAlarmImagePreviewContentPreview() {
    TosswatchTheme {
        WatchAlarmImagePreviewContent(customImagePaths = emptyMap())
    }
}
