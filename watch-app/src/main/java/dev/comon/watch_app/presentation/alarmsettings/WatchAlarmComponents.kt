package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.comon.toss_watch.core.model.watch.WatchAlarmOperation
import dev.comon.watch_app.R

@Composable
internal fun AlarmSettingsScaffold(title: String, content: TransformingLazyColumnScope.() -> Unit) {
    AppScaffold { AlarmSettingsList(title, content) }
}

// AppScaffold 없이 ScreenScaffold + TransformingLazyColumn만 그린다. 페이저의 한 페이지처럼
// 이미 AppScaffold 안에 있는 곳에서 재사용한다.
@Composable
internal fun AlarmSettingsList(title: String, content: TransformingLazyColumnScope.() -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        // 뷰포트는 원형 화면 전체를 쓰고, 화면 형태에 맞춘 여백은 리스트 contentPadding으로만 준다.
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                val spec = rememberTransformationSpec()
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text(title, textAlign = TextAlign.Center) }
            }
            content()
        }
    }
}

// 설정 목록 항목들은 원형 화면 위/아래 가장자리로 스크롤될 때 줄어들고 흐려지도록
// TransformingLazyColumn 항목 스코프에서 TransformationSpec을 적용한다.
@Composable
internal fun TransformingLazyColumnItemScope.SettingsButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val spec = rememberTransformationSpec()
    Button(onClick = onClick, enabled = enabled,
        modifier = Modifier.transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec)) {
        Text(label, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun TransformingLazyColumnItemScope.SettingsText(text: String) {
    val spec = rememberTransformationSpec()
    val transformation = SurfaceTransformation(spec)
    Text(text,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, spec)
            .graphicsLayer {
                with(transformation) {
                    applyContainerTransformation()
                    applyContentTransformation()
                }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun TransformingLazyColumnItemScope.SyncStatus(state: WatchAlarmUiState) {
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
