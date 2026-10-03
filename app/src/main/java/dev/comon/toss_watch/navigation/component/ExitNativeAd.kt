package dev.comon.toss_watch.navigation.component

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import dev.comon.toss_watch.BuildConfig
import dev.comon.toss_watch.R

/** 네이티브 광고는 로드 후 1시간이 지나면 만료되므로, 그 전에 새로 받아 온다. */
private const val MAX_AD_AGE_MILLIS = 55 * 60 * 1000L

private const val CALL_TO_ACTION_CORNER_DP = 22f
private const val BADGE_CORNER_DP = 4f
private const val BADGE_STROKE_DP = 1f

/**
 * 앱 종료 다이얼로그([dev.comon.toss_watch.navigation.ExitAppDialog])에 보여 줄 AdMob 네이티브
 * 광고(고급형)의 소유자.
 *
 * 다이얼로그가 뜨는 순간에 로드를 시작하면 광고가 도착하기 전에 사용자가 종료해 버리므로,
 * 루트 화면에 있는 동안 [preload]로 미리 받아 둔다. 아직 못 받았거나 실패했으면 [nativeAd]가
 * null이고, 다이얼로그는 기존 브랜드 로고를 대신 보여 준다.
 *
 * @param context 광고를 로드할 Activity 컨텍스트.
 */
@Stable
class ExitNativeAdState internal constructor(private val context: Context) {
    /** 지금 보여 줄 수 있는 광고 — 없으면 null. */
    var nativeAd: NativeAd? by mutableStateOf(null)
        private set

    private var isLoading = false
    private var isDestroyed = false
    private var loadedAtMillis = 0L

    /** 쓸 수 있는 광고가 없거나 만료가 가까우면 새로 로드한다. 이미 로드 중이면 아무것도 하지 않는다. */
    internal fun preload() {
        if (isLoading || isDestroyed) return
        val isFresh = nativeAd != null &&
            SystemClock.elapsedRealtime() - loadedAtMillis < MAX_AD_AGE_MILLIS
        if (isFresh) return

        isLoading = true
        AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_UNIT_ID)
            .forNativeAd { ad ->
                isLoading = false
                if (isDestroyed) {
                    ad.destroy()
                    return@forNativeAd
                }
                nativeAd?.destroy()
                nativeAd = ad
                loadedAtMillis = SystemClock.elapsedRealtime()
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // 실패하면 다이얼로그가 브랜드 로고를 보여 주고, 다음 preload() 때 다시 시도한다.
                        isLoading = false
                    }
                },
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** 다이얼로그에서 한 번 보여 준 광고를 버리고 다음에 보여 줄 광고를 새로 받아 둔다. */
    internal fun discardAndReload() {
        val shown = nativeAd ?: return
        nativeAd = null
        shown.destroy()
        preload()
    }

    internal fun destroy() {
        isDestroyed = true
        nativeAd?.destroy()
        nativeAd = null
    }
}

/**
 * [ExitNativeAdState]를 만들어 기억한다. 이 컴포저블이 컴포지션에서 빠질 때(Activity 종료)
 * 들고 있던 광고를 해제한다.
 */
@Composable
fun rememberExitNativeAdState(): ExitNativeAdState {
    val context = LocalContext.current
    val state = remember(context) { ExitNativeAdState(context) }
    DisposableEffect(state) {
        onDispose { state.destroy() }
    }
    return state
}

/**
 * [nativeAd]를 그리는 네이티브 광고(고급형) 뷰.
 *
 * 클릭/노출이 집계되려면 광고 요소들이 [NativeAdView] 안에 있고 각각 등록되어 있어야 해서,
 * Compose로 그리지 않고 XML 레이아웃(`R.layout.exit_native_ad`)을 [AndroidView]로 감싼다.
 * 색상만 [MaterialTheme]에서 가져와 입혀 라이트/다크 테마를 따라간다.
 */
@Composable
fun ExitNativeAd(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val colors = ExitNativeAdColors(
        headline = colorScheme.onSurface.toArgb(),
        body = colorScheme.onSurfaceVariant.toArgb(),
        badge = colorScheme.outline.toArgb(),
        callToActionBackground = colorScheme.primary.toArgb(),
        callToActionText = colorScheme.onPrimary.toArgb(),
    )

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.exit_native_ad, null) as NativeAdView
        },
        update = { adView -> adView.bind(nativeAd, colors) },
        // NativeAdView만 해제한다 — NativeAd 자체는 ExitNativeAdState가 소유한다.
        onRelease = { adView -> adView.destroy() },
    )
}

private data class ExitNativeAdColors(
    val headline: Int,
    val body: Int,
    val badge: Int,
    val callToActionBackground: Int,
    val callToActionText: Int,
)

/** 광고 요소를 뷰에 채우고 [NativeAdView]에 등록한다. 광고에 없는 요소는 숨긴다. */
private fun NativeAdView.bind(nativeAd: NativeAd, colors: ExitNativeAdColors) {
    val density = resources.displayMetrics.density
    val iconView = findViewById<ImageView>(R.id.ad_icon)
    val badgeView = findViewById<TextView>(R.id.ad_badge)
    val headlineTextView = findViewById<TextView>(R.id.ad_headline)
    val bodyTextView = findViewById<TextView>(R.id.ad_body)
    val adMediaView = findViewById<MediaView>(R.id.ad_media)
    val callToActionTextView = findViewById<TextView>(R.id.ad_call_to_action)

    badgeView.setTextColor(colors.badge)
    badgeView.background = GradientDrawable().apply {
        cornerRadius = BADGE_CORNER_DP * density
        setStroke((BADGE_STROKE_DP * density).toInt(), colors.badge)
    }

    headlineTextView.text = nativeAd.headline
    headlineTextView.setTextColor(colors.headline)

    bodyTextView.text = nativeAd.body
    bodyTextView.setTextColor(colors.body)
    bodyTextView.visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE

    val icon = nativeAd.icon?.drawable
    iconView.setImageDrawable(icon)
    iconView.visibility = if (icon == null) View.GONE else View.VISIBLE

    nativeAd.mediaContent?.let { adMediaView.mediaContent = it }

    callToActionTextView.text = nativeAd.callToAction
    callToActionTextView.setTextColor(colors.callToActionText)
    callToActionTextView.background = GradientDrawable().apply {
        cornerRadius = CALL_TO_ACTION_CORNER_DP * density
        setColor(colors.callToActionBackground)
    }
    callToActionTextView.visibility =
        if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE

    headlineView = headlineTextView
    bodyView = bodyTextView
    this.iconView = iconView
    mediaView = adMediaView
    callToActionView = callToActionTextView

    // 요소 등록을 모두 마친 뒤에 호출해야 한다 — 이 시점에 클릭/노출 추적이 연결된다.
    setNativeAd(nativeAd)
}
