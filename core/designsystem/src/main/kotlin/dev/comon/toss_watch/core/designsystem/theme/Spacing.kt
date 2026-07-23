package dev.comon.toss_watch.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Toss Watch 공용 스페이싱 스케일. DESIGN.md `spacing` 토큰을 그대로 반영한 8pt 그리드다.
 * 테마에 따라 달라지는 값이 아니므로 CompositionLocal 없이 top-level 상수로 노출한다.
 */
object TossSpacing {
    /** 모바일 화면 좌우 여백. */
    val containerMargin: Dp = 20.dp

    /** 그리드 컬럼 사이 간격. */
    val gutter: Dp = 16.dp

    /** 같은 그룹 내 밀접한 요소 사이 간격. */
    val stackSm: Dp = 8.dp

    /** 연관된 요소 묶음 사이 간격. */
    val stackMd: Dp = 16.dp

    /** 구분되는 섹션 사이 간격. */
    val stackLg: Dp = 24.dp

    /** 섹션 내부 상하 패딩. */
    val sectionPadding: Dp = 32.dp
}
