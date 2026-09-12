package dev.comon.watch_app.presentation.alarmsettings

import dev.comon.toss_watch.core.common.mvi.UiIntent
import dev.comon.toss_watch.core.common.mvi.UiSideEffect
import dev.comon.toss_watch.core.common.mvi.UiState
import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.toss_watch.core.model.watch.WatchAlarmOperation
import dev.comon.watch_app.domain.repository.WatchAlarmSyncState

data class WatchAlarmUiState(
    val sync: WatchAlarmSyncState = WatchAlarmSyncState(),
    val hour: Int = 9,
    val minute: Int = 0,
    val days: Set<Int> = (0..5).toSet(),
    val submitting: Boolean = false,
) : UiState {
    val busy: Boolean get() = submitting ||
        (sync.pending != null && sync.pending.operation != WatchAlarmOperation.REFRESH)
}

sealed interface WatchAlarmIntent : UiIntent {
    data object Refresh : WatchAlarmIntent
    data object NewAlarm : WatchAlarmIntent
    data class SetTime(val hour: Int, val minute: Int) : WatchAlarmIntent
    data class ToggleDay(val day: Int) : WatchAlarmIntent
    data class Add(val stockCode: String, val stockName: String) : WatchAlarmIntent
    data class Toggle(val alarm: WatchAlarm) : WatchAlarmIntent
    data class Delete(val alarmId: Long) : WatchAlarmIntent
}

sealed interface WatchAlarmEffect : UiSideEffect {
    data object Submitted : WatchAlarmEffect
}
