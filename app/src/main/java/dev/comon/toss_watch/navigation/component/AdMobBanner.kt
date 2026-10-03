package dev.comon.toss_watch.navigation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dev.comon.toss_watch.BuildConfig

/** 프리뷰(인스펙션 모드)에서 광고 자리 표시용으로 쓰는 높이 — 폰 폭의 adaptive 배너 높이와 비슷하다. */
private val PREVIEW_BANNER_HEIGHT = 56.dp

/**
 * AdMob adaptive 배너.
 *
 * [dev.comon.toss_watch.navigation.BottomMenuScreen]의 `Scaffold(topBar)` 슬롯에서 공통 탑바
 * 바로 아래에 놓인다. Mobile Ads SDK에는 Compose 네이티브 API가 없어 [AdView]를 [AndroidView]로
 * 감싼다.
 *
 * 광고가 로드되기 전에도 배너 높이를 미리 확보한다 — 가용 폭으로 adaptive 배너 크기([AdSize])를
 * 먼저 계산해 그 높이로 자리를 고정해 두지 않으면, 광고가 뒤늦게 로드되는 순간 `topBar` 슬롯
 * 높이가 바뀌어 아래 리스트가 튄다. 폭이 바뀌면(회전, 창 크기 변경) 크기를 다시 계산하고
 * [AdView]를 새로 만든다 — [AdView]의 광고 크기는 한 번만 설정할 수 있다.
 *
 * 광고 단위 ID는 `BuildConfig.ADMOB_BANNER_UNIT_ID`(app/build.gradle.kts가 local.properties의
 * `tossWatch.adMobBannerUnitId`에서 주입, debug는 항상 Google 테스트 ID)를 쓴다.
 */
@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (LocalInspectionMode.current) {
            // 프리뷰에서는 Mobile Ads SDK를 쓸 수 없으므로 자리만 차지한다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PREVIEW_BANNER_HEIGHT)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            val context = LocalContext.current
            val widthDp = maxWidth.value.toInt()
            val adSize = remember(context, widthDp) {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            }

            key(adSize) {
                val adView = remember {
                    AdView(context).apply {
                        adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                        setAdSize(adSize)
                        loadAd(AdRequest.Builder().build())
                    }
                }

                // 화면이 보이지 않는 동안에는 광고 갱신을 멈추고, 컴포지션에서 빠질 때 뷰를 해제한다.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, adView) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> adView.resume()
                            Lifecycle.Event.ON_PAUSE -> adView.pause()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        adView.destroy()
                    }
                }

                AndroidView(
                    factory = { adView },
                    modifier = Modifier
                        .width(adSize.width.dp)
                        .height(adSize.height.dp),
                )
            }
        }
    }
}
