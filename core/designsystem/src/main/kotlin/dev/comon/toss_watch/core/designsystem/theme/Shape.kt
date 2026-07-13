package dev.comon.toss_watch.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Toss Watch 공용 셰이프 스케일.
 * 버튼/카드류는 medium~large, 다이얼로그·바텀시트는 extraLarge를 사용한다.
 */
val TossWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
