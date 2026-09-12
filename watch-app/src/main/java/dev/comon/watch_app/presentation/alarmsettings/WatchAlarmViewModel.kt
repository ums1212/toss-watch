package dev.comon.watch_app.presentation.alarmsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.comon.toss_watch.core.common.mvi.BaseMviViewModel
import dev.comon.toss_watch.core.model.watch.WatchAlarmOperation
import dev.comon.toss_watch.core.model.watch.WatchAlarmRequest
import dev.comon.watch_app.domain.usecase.ObserveWatchAlarmsUseCase
import dev.comon.watch_app.domain.usecase.RefreshWatchAlarmsUseCase
import dev.comon.watch_app.domain.usecase.SaveWatchAlarmUseCase
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class WatchAlarmViewModel @Inject constructor(
    observe: ObserveWatchAlarmsUseCase,
    private val refresh: RefreshWatchAlarmsUseCase,
    private val save: SaveWatchAlarmUseCase,
    private val savedState: SavedStateHandle,
) : BaseMviViewModel<WatchAlarmUiState, WatchAlarmIntent, WatchAlarmEffect>(
    WatchAlarmUiState(
        hour = savedState["hour"] ?: 9,
        minute = savedState["minute"] ?: 0,
        days = savedState.get<IntArray>("days")?.toSet() ?: (0..5).toSet(),
    ),
) {
    init {
        viewModelScope.launch { observe().collect { sync -> updateState { copy(sync = sync) } } }
    }

    override fun handleIntent(intent: WatchAlarmIntent) {
        when (intent) {
            WatchAlarmIntent.Refresh -> {
                if (uiState.value.submitting) return
                updateState { copy(submitting = true) }
                viewModelScope.launch {
                    try { refresh() } finally { updateState { copy(submitting = false) } }
                }
            }
            WatchAlarmIntent.NewAlarm -> setDraft(9, 0, (0..5).toSet())
            is WatchAlarmIntent.SetTime -> if (intent.hour in 0..23 && intent.minute in 0..59) {
                setDraft(intent.hour, intent.minute, uiState.value.days)
            }
            is WatchAlarmIntent.ToggleDay -> if (intent.day in 0..6) {
                val state = uiState.value
                setDraft(state.hour, state.minute,
                    if (intent.day in state.days) state.days - intent.day else state.days + intent.day)
            }
            is WatchAlarmIntent.Add -> submit(WatchAlarmOperation.ADD, intent.stockCode, intent.stockName)
            is WatchAlarmIntent.Toggle -> submit(WatchAlarmOperation.TOGGLE,
                alarmId = intent.alarm.id, enabled = !intent.alarm.enabled)
            is WatchAlarmIntent.Delete -> submit(WatchAlarmOperation.DELETE, alarmId = intent.alarmId)
        }
    }

    private fun setDraft(hour: Int, minute: Int, days: Set<Int>) {
        savedState["hour"] = hour
        savedState["minute"] = minute
        savedState["days"] = days.toIntArray()
        updateState { copy(hour = hour, minute = minute, days = days) }
    }

    private fun submit(operation: WatchAlarmOperation, code: String = "", name: String = "",
        alarmId: Long = 0, enabled: Boolean = true) {
        val state = uiState.value
        val snapshot = state.sync.snapshot ?: return
        if (state.busy || !snapshot.available || (operation == WatchAlarmOperation.ADD && state.days.isEmpty())) return
        val request = WatchAlarmRequest(id = UUID.randomUUID().toString(), uuid = snapshot.uuid,
            session = snapshot.session, operation = operation, stockCode = code, stockName = name,
            alarmId = alarmId, hour = state.hour, minute = state.minute, days = state.days.sorted(), enabled = enabled)
        updateState { copy(submitting = true) }
        viewModelScope.launch {
            try {
                if (save(request) && operation == WatchAlarmOperation.ADD) sendSideEffect(WatchAlarmEffect.Submitted)
            } finally { updateState { copy(submitting = false) } }
        }
    }
}
