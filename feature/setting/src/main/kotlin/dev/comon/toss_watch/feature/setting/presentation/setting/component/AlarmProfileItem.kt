package dev.comon.toss_watch.feature.setting.presentation.setting.component

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
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile

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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alarm.stockName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${alarm.stockCode} · 매일 ${"%02d:%02d".format(alarm.hour, alarm.minute)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                isEnabled = true,
            ),
            onToggle = {},
            onDelete = {},
            enabled = true,
        )
    }
}
