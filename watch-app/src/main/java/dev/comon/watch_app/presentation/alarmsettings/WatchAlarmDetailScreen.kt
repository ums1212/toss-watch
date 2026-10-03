package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import dev.comon.toss_watch.core.model.MAX_ALARM_COUNT
import dev.comon.watch_app.R

@Composable
fun WatchAlarmDetailScreen(code: String, name: String, state: WatchAlarmUiState,
    onIntent: (WatchAlarmIntent) -> Unit, onBack: () -> Unit, onAdd: () -> Unit) {
    var deleting by rememberSaveable(code) { mutableStateOf<Long?>(null) }
    val snapshot = state.sync.snapshot
    val alarms = snapshot?.alarms.orEmpty().filter { it.stockCode == code }.sortedWith(compareBy({ it.hour }, { it.minute }))
    val enabled = snapshot?.available == true && !state.busy
    // 종목과 무관한 전체 개수 기준 — available일 때 스냅샷의 알람 목록은 잘리지 않은 전체다.
    val limitReached = snapshot?.alarms.orEmpty().size >= MAX_ALARM_COUNT
    AlarmSettingsScaffold(name) {
        item { SyncStatus(state) }
        if (limitReached) item { SettingsText(stringResource(R.string.alarm_sync_limit)) }
        if (alarms.isEmpty()) item { SettingsText(stringResource(R.string.alarm_empty)) }
        alarms.forEach { alarm ->
            item(key = "alarm_${alarm.id}") {
                WatchAlarmCard(
                    alarm = alarm,
                    enabled = enabled,
                    deleting = deleting == alarm.id,
                    onToggle = { onIntent(WatchAlarmIntent.Toggle(alarm)) },
                    onDeleteRequest = { deleting = alarm.id },
                    onDeleteConfirm = { onIntent(WatchAlarmIntent.Delete(alarm.id)); deleting = null },
                    onDeleteCancel = { deleting = null },
                )
            }
        }
        item(key = "actions") {
            SettingsIconButtonRow {
                SettingsIconButton(R.drawable.ic_add, stringResource(R.string.alarm_add),
                    enabled && !limitReached && snapshot?.stocks.orEmpty().any { it.code == code }, onAdd)
                SettingsIconButton(R.drawable.ic_refresh, stringResource(R.string.alarm_sync_refresh), !state.submitting) {
                    onIntent(WatchAlarmIntent.Refresh)
                }
            }
        }
        item { SettingsButton(stringResource(R.string.alarm_back), onClick = onBack) }
    }
}
