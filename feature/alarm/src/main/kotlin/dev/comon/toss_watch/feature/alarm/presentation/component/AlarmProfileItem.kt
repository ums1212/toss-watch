package dev.comon.toss_watch.feature.alarm.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile

/** 알림 프로필 1행 — 종목, 알림 시각, 활성 토글 스위치, 삭제 버튼. */
@Composable
fun AlarmProfileItem(
    alarm: AlarmProfile,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TossSpacing.stackSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${formatDaysOfWeek(alarm.daysOfWeek)} " +
                    "%02d:%02d".format(alarm.hour, alarm.minute),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (alarm.disabledReason.isNotBlank()) {
                Text(
                    text = alarm.disabledReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "알림 삭제",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = onToggle,
            enabled = enabled,
        )
    }
}

/** 요일 라벨 — index가 곧 서버 계약의 요일 값(0=월 ~ 6=일). */
private val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")
private val WEEKDAYS = listOf(0, 1, 2, 3, 4)
private val WEEKENDS = listOf(5, 6)
private val EVERY_DAY = listOf(0, 1, 2, 3, 4, 5, 6)

/** 알림 요일 리스트를 표시용 라벨로 변환한다 — 매일/평일/주말은 축약 표기, 그 외는 개별 나열. */
internal fun formatDaysOfWeek(daysOfWeek: List<Int>): String {
    val sorted = daysOfWeek.sorted()
    return when (sorted) {
        EVERY_DAY -> "매일"
        WEEKDAYS -> "평일"
        WEEKENDS -> "주말"
        else -> sorted.joinToString("·") { DAY_LABELS[it] }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmProfileItemPreview() {
    TossWatchTheme {
        AlarmProfileItem(
            alarm = AlarmProfile(
                id = 1L,
                stockCode = "005930",
                stockName = "삼성전자",
                hour = 9,
                minute = 30,
                daysOfWeek = listOf(0, 1, 2, 3, 4),
                isEnabled = true,
            ),
            onToggle = {},
            onDelete = {},
            enabled = true,
        )
    }
}
