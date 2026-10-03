package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.comon.toss_watch.core.model.MAX_ALARM_COUNT
import dev.comon.watch_app.R
import java.util.Locale

@Composable
fun WatchAddAlarmScreen(code: String, name: String, state: WatchAlarmUiState,
    onIntent: (WatchAlarmIntent) -> Unit, onBack: () -> Unit, onPickTime: () -> Unit) {
    val labels = stringArrayResource(R.array.alarm_weekdays)
    val limitReached = state.sync.snapshot?.alarms.orEmpty().size >= MAX_ALARM_COUNT
    AlarmSettingsScaffold(stringResource(R.string.alarm_add)) {
        item { SettingsText(name) }
        item {
            SettingsButton(stringResource(R.string.alarm_time,
                String.format(Locale.ROOT, "%02d:%02d", state.hour, state.minute)), !state.busy, onPickTime)
        }
        item { SettingsText(stringResource(R.string.alarm_timezone)) }
        item { SettingsText(stringResource(R.string.alarm_days)) }
        (0..6).chunked(2).forEach { days ->
            item {
                val spec = rememberTransformationSpec()
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.transformedHeight(this, spec)) {
                    days.forEach { day ->
                        Button(onClick = { onIntent(WatchAlarmIntent.ToggleDay(day)) }, enabled = !state.busy,
                            modifier = Modifier.weight(1f).semantics { selected = day in state.days },
                            transformation = SurfaceTransformation(spec)) {
                            Text((if (day in state.days) "✓ " else "") + labels[day])
                        }
                    }
                }
            }
        }
        if (state.days.isEmpty()) item { SettingsText(stringResource(R.string.alarm_select_day)) }
        item { SyncStatus(state) }
        if (limitReached) item { SettingsText(stringResource(R.string.alarm_sync_limit)) }
        item {
            SettingsButton(stringResource(R.string.alarm_save), !state.busy && state.days.isNotEmpty() && !limitReached &&
                state.sync.snapshot?.available == true && state.sync.snapshot.stocks.any { it.code == code }) {
                onIntent(WatchAlarmIntent.Add(code, name))
            }
        }
        item { SettingsButton(stringResource(R.string.alarm_cancel), onClick = onBack) }
    }
}
