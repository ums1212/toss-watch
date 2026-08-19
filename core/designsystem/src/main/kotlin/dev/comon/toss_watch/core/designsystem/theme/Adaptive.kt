package dev.comon.toss_watch.core.designsystem.theme

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * 앱이 인지하는 3단계 창 폭 브레이크포인트.
 *
 * [androidx.window.core.layout.WindowSizeClass]의 표준 breakpoint(600dp/840dp)를 그대로 따른다 —
 * deprecated된 `material3-window-size-class`의 `WindowWidthSizeClass` 대신 이 값을 쓴다.
 */
enum class TossWindowWidth {
    /** 휴대폰 세로 모드 등 <600dp. 기존 플로팅 하단 탭 바 레이아웃이 유지되는 구간. */
    COMPACT,

    /** 태블릿 세로/폴더블 펼침 등 600~839dp. Navigation Rail이 적용되는 구간. */
    MEDIUM,

    /** 태블릿 가로/데스크톱 창 등 840dp+. Wide Navigation Rail + List-Detail이 적용되는 구간. */
    EXPANDED,
}

/**
 * 현재 창 폭 구간. [dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme]가
 * [androidx.compose.material3.adaptive.currentWindowAdaptiveInfo]를 계산해 provide한다.
 * 기본값 COMPACT는 프리뷰 등 Theme 바깥에서 실수로 참조된 경우의 안전한 폴백이다.
 */
val LocalTossWindowWidth = staticCompositionLocalOf { TossWindowWidth.COMPACT }

/** [WindowSizeClass]를 [TossWindowWidth] 3단계로 환산한다. */
internal fun WindowSizeClass.toTossWindowWidth(): TossWindowWidth = when {
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> TossWindowWidth.EXPANDED
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> TossWindowWidth.MEDIUM
    else -> TossWindowWidth.COMPACT
}

/** 현재 [androidx.compose.ui.window] 호스트의 창 폭을 [TossWindowWidth]로 계산한다. */
@Composable
internal fun calculateTossWindowWidth(): TossWindowWidth =
    currentWindowAdaptiveInfo().windowSizeClass.toTossWindowWidth()

/**
 * [TossSpacing]이 화면 크기와 무관한 고정 간격 스케일인 것과 달리, 창 폭 구간에 따라
 * 값 자체가 달라지는 레이아웃 토큰. [LocalTossWindowWidth]를 읽어 계산한다.
 */
object TossAdaptive {
    /** 콘텐츠 좌우 여백. COMPACT는 [TossSpacing.containerMargin]과 동일한 20dp에서 시작해 넓어진다. */
    val containerMargin: Dp
        @Composable
        get() = when (LocalTossWindowWidth.current) {
            TossWindowWidth.COMPACT -> TossSpacing.containerMargin
            TossWindowWidth.MEDIUM -> 32.dp
            TossWindowWidth.EXPANDED -> 40.dp
        }

    /**
     * 콘텐츠 컬럼의 최대 폭. 큰 화면에서 카드가 화면 전체로 늘어나 읽기 어려워지는 것을 막는다.
     * COMPACT는 제한이 없다([Dp.Unspecified]).
     */
    val contentMaxWidth: Dp
        @Composable
        get() = when (LocalTossWindowWidth.current) {
            TossWindowWidth.COMPACT -> Dp.Unspecified
            TossWindowWidth.MEDIUM -> 720.dp
            TossWindowWidth.EXPANDED -> 900.dp
        }

    /** 보유종목/알림 카드 리스트를 나열할 컬럼 수. */
    val holdingColumns: Int
        @Composable
        get() = when (LocalTossWindowWidth.current) {
            TossWindowWidth.COMPACT -> 1
            TossWindowWidth.MEDIUM, TossWindowWidth.EXPANDED -> 2
        }
}

/**
 * 콘텐츠 폭을 [TossAdaptive.contentMaxWidth]로 제한하고 가로 중앙 정렬한다.
 * COMPACT에서는 제한이 없어(`widthIn(max = Unspecified)`는 no-op) 기존 레이아웃과 동일하게 동작한다.
 */
@Composable
fun Modifier.adaptiveContentWidth(): Modifier = this.widthIn(max = TossAdaptive.contentMaxWidth)
