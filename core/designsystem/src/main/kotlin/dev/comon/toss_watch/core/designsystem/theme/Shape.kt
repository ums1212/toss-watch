package dev.comon.toss_watch.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Toss Watch 공용 셰이프 스케일. DESIGN.md `rounded` 토큰(sm~xl)과 1:1로 대응한다.
 * 버튼/카드류는 medium~large, 다이얼로그·바텀시트는 extraLarge를 사용한다.
 * 등락 Status Pill(`rounded.full` = 999px)은 M3 [Shapes] 슬롯이 없으므로
 * 사용처에서 `RoundedCornerShape(percent = 50)`을 직접 사용한다.
 */
val TossWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
