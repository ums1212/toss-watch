package dev.comon.toss_watch.feature.setting.presentation.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/** 요일 라벨 — index가 곧 서버 계약의 요일 값(0=월 ~ 6=일, Python `date.weekday()` 기준). */
private val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

/**
 * 알림 요일 선택 — 월~일 7개 원형 토글을 가로로 나열한다.
 * 선택된 요일은 primary 배경 + onPrimary 텍스트, 미선택은 surfaceVariant 배경 + onSurfaceVariant 텍스트.
 *
 * @param selectedDays 현재 선택된 요일 값 집합 (0=월 ~ 6=일)
 * @param onToggleDay 요일 원을 탭했을 때 그 요일 값을 전달한다. 선택/해제 여부는 호출부가 판단한다.
 */
@Composable
fun DayOfWeekSelector(
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TossSpacing.stackSm),
    ) {
        DAY_LABELS.forEachIndexed { day, label ->
            val isSelected = selectedDays.contains(day)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .clickable { onToggleDay(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DayOfWeekSelectorPreview() {
    TossWatchTheme {
        DayOfWeekSelector(
            selectedDays = setOf(0, 1, 2, 3, 4, 5),
            onToggleDay = {},
        )
    }
}
