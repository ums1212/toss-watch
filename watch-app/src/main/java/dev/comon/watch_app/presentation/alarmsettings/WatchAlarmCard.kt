package dev.comon.watch_app.presentation.alarmsettings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.watch_app.R
import java.util.Locale

// 알람 하나를 카드 한 장으로 묶어, 알람이 여러 개일 때도 알람 간 경계가 분명히 보이도록 한다.
// 카드 자체는 누를 수 없고(내부 아이콘 버튼과 누름 영역이 겹치지 않도록), 원형 화면 가장자리에서는
// SettingsText와 같은 방식으로 줄어들고 흐려진다.
@Composable
internal fun TransformingLazyColumnItemScope.WatchAlarmCard(
    alarm: WatchAlarm,
    enabled: Boolean,
    deleting: Boolean,
    onToggle: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
) {
    val spec = rememberTransformationSpec()
    val transformation = SurfaceTransformation(spec)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, spec)
            .graphicsLayer {
                with(transformation) {
                    applyContainerTransformation()
                    applyContentTransformation()
                }
            }
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            String.format(Locale.ROOT, "%02d:%02d", alarm.hour, alarm.minute),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            alarmDays(alarm.days),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (alarm.disabledReason.isNotBlank()) {
            Text(
                alarm.disabledReason,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (deleting) {
            AlarmDeleteConfirm(enabled, onDeleteConfirm, onDeleteCancel)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // 켜짐 = 종 아이콘(채워진 강조색), 꺼짐 = 빗금 친 종 아이콘. 설명은 기존 문구를 접근성 라벨로 재사용한다.
                IconToggleButton(checked = alarm.enabled, onCheckedChange = { onToggle() }, enabled = enabled) {
                    Icon(
                        painter = painterResource(if (alarm.enabled) R.drawable.ic_alarm_on else R.drawable.ic_alarm_off),
                        contentDescription = stringResource(if (alarm.enabled) R.string.alarm_disable else R.string.alarm_enable),
                    )
                }
                FilledTonalIconButton(onClick = onDeleteRequest, enabled = enabled) {
                    Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.alarm_delete))
                }
            }
        }
    }
}

@Composable
private fun AlarmDeleteConfirm(enabled: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Text(
        stringResource(R.string.alarm_delete_question),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Button(onClick = onConfirm, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )) {
        Text(stringResource(R.string.alarm_delete_confirm), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(4.dp))
    Button(onClick = onCancel, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.filledTonalButtonColors()) {
        Text(stringResource(R.string.alarm_cancel), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// 알람 추가 / 새로고침 / 설정처럼 화면 하단의 보조 동작을 아이콘 버튼 한 줄로 나란히 보여준다.
@Composable
internal fun TransformingLazyColumnItemScope.SettingsIconButtonRow(content: @Composable () -> Unit) {
    val spec = rememberTransformationSpec()
    val transformation = SurfaceTransformation(spec)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, spec)
            .graphicsLayer {
                with(transformation) {
                    applyContainerTransformation()
                    applyContentTransformation()
                }
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
internal fun SettingsIconButton(@DrawableRes icon: Int, contentDescription: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled) {
        Icon(painter = painterResource(icon), contentDescription = contentDescription)
    }
}
