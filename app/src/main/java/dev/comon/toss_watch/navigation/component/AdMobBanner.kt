package dev.comon.toss_watch.navigation.component

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
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
 * [AdMobBanner]가 보여 주는 [AdView]의 소유자.
 *
 * [AdView]를 [AdMobBanner] 안에서 `remember`하면, Navigation 3의 `NavDisplay`가 백스택 최상단이
 * 아닌 엔트리를 컴포지션에서 빼는 순간 뷰가 함께 버려진다 — 설정/상세 화면에 갔다 돌아올 때마다
 * 광고를 처음부터 다시 요청하게 된다. 그래서 뷰는 `NavDisplay` 바깥([rememberAdMobBannerState])에서
 * 만든 이 홀더가 쥐고, [AdMobBanner]는 화면에 붙였다 떼기만 한다.
 * 자세한 경위는 `docs/admob-banner-reload-on-navigation.md` 참고.
 *
 * @param context [AdView]를 만들 Activity 컨텍스트.
 */
@Stable
class AdMobBannerState internal constructor(private val context: Context) {
    private var adView: AdView? = null
    private var adSize: AdSize? = null
    private var isHostResumed = false
    private var isAttached = false

    /**
     * [adSize] 크기의 [AdView]를 돌려준다. 같은 크기로 이미 만든 뷰가 있으면 광고가 로드된 그 뷰를
     * 재사용하고, 크기가 달라졌으면(회전, 창 크기 변경) 버리고 새로 만든다 — [AdView]의 광고 크기는
     * 한 번만 설정할 수 있다.
     */
    internal fun obtain(adSize: AdSize): AdView {
        adView?.let { current ->
            if (this.adSize == adSize) return current
            current.destroy()
        }
        return AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            setAdSize(adSize)
            loadAd(AdRequest.Builder().build())
        }.also {
            adView = it
            this.adSize = adSize
        }
    }

    internal fun onAttached() {
        isAttached = true
        updateRefresh()
    }

    internal fun onDetached() {
        isAttached = false
        updateRefresh()
    }

    internal fun onHostResumed(resumed: Boolean) {
        isHostResumed = resumed
        updateRefresh()
    }

    internal fun destroy() {
        adView?.destroy()
        adView = null
        adSize = null
    }

    // 배너가 실제로 화면에 붙어 있고 Activity가 resumed일 때만 광고를 갱신한다 — 다른 화면에 가려져
    // 있는 동안 갱신하면 보이지 않는 노출이 쌓인다.
    private fun updateRefresh() {
        val view = adView ?: return
        if (isHostResumed && isAttached) view.resume() else view.pause()
    }
}

/**
 * [AdMobBannerState]를 만들어 기억한다. 화면 전환에도 [AdView]가 살아남도록 반드시 `NavDisplay`
 * 바깥에서 호출해야 한다. 이 컴포저블이 컴포지션에서 빠질 때(Activity 종료)에만 뷰를 해제한다.
 */
@Composable
fun rememberAdMobBannerState(): AdMobBannerState {
    val context = LocalContext.current
    val state = remember(context) { AdMobBannerState(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> state.onHostResumed(true)
                Lifecycle.Event.ON_PAUSE -> state.onHostResumed(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.destroy()
        }
    }
    return state
}

/**
 * AdMob adaptive 배너.
 *
 * [dev.comon.toss_watch.navigation.BottomMenuScreen]의 `Scaffold(topBar)` 슬롯에서 공통 탑바
 * 바로 아래에 놓인다. Mobile Ads SDK에는 Compose 네이티브 API가 없어 [AdView]를 [AndroidView]로
 * 감싼다. [AdView] 자체는 [state]가 소유하고, 이 컴포저블은 그 뷰를 붙였다 떼기만 한다.
 *
 * 광고가 로드되기 전에도 배너 높이를 미리 확보한다 — 가용 폭으로 adaptive 배너 크기([AdSize])를
 * 먼저 계산해 그 높이로 자리를 고정해 두지 않으면, 광고가 뒤늦게 로드되는 순간 `topBar` 슬롯
 * 높이가 바뀌어 아래 리스트가 튄다.
 *
 * 광고 단위 ID는 `BuildConfig.ADMOB_BANNER_UNIT_ID`(app/build.gradle.kts가 local.properties의
 * `tossWatch.adMobBannerUnitId`에서 주입, debug는 항상 Google 테스트 ID)를 쓴다.
 */
@Composable
fun AdMobBanner(
    state: AdMobBannerState,
    modifier: Modifier = Modifier,
) {
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
            val adView = remember(state, adSize) { state.obtain(adSize) }

            DisposableEffect(state, adView) {
                state.onAttached()
                onDispose { state.onDetached() }
            }

            // AndroidView의 factory는 노드당 한 번만 호출되므로, 크기가 바뀌어 뷰가 교체되면
            // key로 노드를 새로 만든다.
            key(adView) {
                AndroidView(
                    factory = {
                        // 이전 화면 진입 때 붙었던 (이미 버려진) AndroidView 컨테이너가 아직 부모로
                        // 남아 있으므로, 먼저 떼어내지 않으면 "already has a parent"로 크래시한다.
                        (adView.parent as? ViewGroup)?.removeView(adView)
                        adView
                    },
                    modifier = Modifier
                        .width(adSize.width.dp)
                        .height(adSize.height.dp),
                )
            }
        }
    }
}
