package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import dev.comon.toss_watch.core.model.watch.WatchAlarmOperation
import dev.comon.watch_app.R

@Composable
internal fun AlarmSettingsScaffold(title: String, content: TransformingLazyColumnScope.() -> Unit) {
    AppScaffold {
        val listState = rememberTransformingLazyColumnState()
        ScreenScaffold(scrollState = listState) { padding ->
            TransformingLazyColumn(state = listState, contentPadding = padding) {
                item { ListHeader { Text(title, textAlign = TextAlign.Center) } }
                content()
            }
        }
    }
}

@Composable
internal fun SettingsButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(label, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun SettingsText(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun SyncStatus(state: WatchAlarmUiState) {
    val error = state.sync.error ?: state.sync.snapshot?.error ?: state.sync.snapshot?.receipt?.error
    val text = when {
        state.sync.error == "DELIVERY_FAILED" -> stringResource(R.string.alarm_sync_delivery_failed)
        state.sync.pending?.operation == WatchAlarmOperation.REFRESH -> stringResource(R.string.alarm_sync_loading)
        state.busy -> stringResource(R.string.alarm_sync_pending)
        error != null -> stringResource(when (error) {
            "PAIR_PHONE" -> R.string.alarm_sync_pair_phone
            "CHECK_PHONE" -> R.string.alarm_sync_unknown
            "STALE_DATA" -> R.string.alarm_sync_stale
            "DELIVERY_FAILED" -> R.string.alarm_sync_delivery_failed
            "TOO_LARGE" -> R.string.alarm_sync_too_large
            else -> R.string.alarm_sync_failed
        })
        state.sync.snapshot?.available != true -> stringResource(R.string.alarm_sync_pair_phone)
        else -> stringResource(R.string.alarm_sync_complete)
    }
    SettingsText(text)
}

@Composable
internal fun alarmDays(days: List<Int>): String {
    val labels = stringArrayResource(R.array.alarm_weekdays)
    return days.sorted().mapNotNull { labels.getOrNull(it) }.joinToString(" · ")
}
