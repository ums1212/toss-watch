package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import dev.comon.watch_app.R
import java.util.Locale

@Composable
fun WatchAlarmDetailScreen(code: String, name: String, state: WatchAlarmUiState,
    onIntent: (WatchAlarmIntent) -> Unit, onBack: () -> Unit, onAdd: () -> Unit) {
    var deleting by rememberSaveable(code) { mutableStateOf<Long?>(null) }
    val snapshot = state.sync.snapshot
    val alarms = snapshot?.alarms.orEmpty().filter { it.stockCode == code }.sortedWith(compareBy({ it.hour }, { it.minute }))
    val enabled = snapshot?.available == true && !state.busy
    AlarmSettingsScaffold(name) {
        item { SyncStatus(state) }
        if (alarms.isEmpty()) item { SettingsText(stringResource(R.string.alarm_empty)) }
        alarms.forEach { alarm ->
            item(key = "time_${alarm.id}") {
                SettingsText(String.format(Locale.ROOT, "%02d:%02d", alarm.hour, alarm.minute) + "\n" + alarmDays(alarm.days))
            }
            if (alarm.disabledReason.isNotBlank()) {
                item(key = "reason_${alarm.id}") { SettingsText(alarm.disabledReason) }
            }
            item(key = "toggle_${alarm.id}") {
                SettingsButton(stringResource(if (alarm.enabled) R.string.alarm_disable else R.string.alarm_enable), enabled) {
                    onIntent(WatchAlarmIntent.Toggle(alarm))
                }
            }
            if (deleting == alarm.id) {
                item { SettingsText(stringResource(R.string.alarm_delete_question)) }
                item {
                    SettingsButton(stringResource(R.string.alarm_delete_confirm), enabled) {
                        onIntent(WatchAlarmIntent.Delete(alarm.id)); deleting = null
                    }
                }
                item { SettingsButton(stringResource(R.string.alarm_cancel)) { deleting = null } }
            } else {
                item(key = "delete_${alarm.id}") {
                    SettingsButton(stringResource(R.string.alarm_delete), enabled) { deleting = alarm.id }
                }
            }
        }
        item {
            SettingsButton(stringResource(R.string.alarm_add), enabled && snapshot?.stocks.orEmpty().any { it.code == code }, onAdd)
        }
        item { SettingsButton(stringResource(R.string.alarm_sync_refresh), !state.submitting) { onIntent(WatchAlarmIntent.Refresh) } }
        item { SettingsButton(stringResource(R.string.alarm_back), onClick = onBack) }
    }
}
