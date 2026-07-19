package dev.comon.toss_watch.feature.setting.presentation.setting

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchButton
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingIndicator
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.setting.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.setting.presentation.SettingUiIntent
import dev.comon.toss_watch.feature.setting.presentation.SettingUiSideEffect
import dev.comon.toss_watch.feature.setting.presentation.SettingUiState
import dev.comon.toss_watch.feature.setting.presentation.SettingViewModel
import dev.comon.toss_watch.feature.setting.presentation.setting.component.AddAlarmDialog
import dev.comon.toss_watch.feature.setting.presentation.setting.component.AlarmProfileItem

/**
 * 알림 스케줄러 + Wear OS 연동 설정.
 *
 * @param onNavigateBack [SettingUiSideEffect.NavigateBack] 수신 시 호출.
 * @param onNavigateToTossKey [SettingUiSideEffect.NavigateToTossKey] 수신 시 호출.
 * @param onNavigateToWatchPair [SettingUiSideEffect.NavigateToWatchPair] 수신 시 호출.
 */
@Composable
fun SettingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTossKey: () -> Unit,
    onNavigateToWatchPair: () -> Unit,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    SettingUiSideEffect.NavigateBack -> onNavigateBack()
                    SettingUiSideEffect.NavigateToTossKey -> onNavigateToTossKey()
                    SettingUiSideEffect.NavigateToWatchPair -> onNavigateToWatchPair()
                    is SettingUiSideEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingContent(
    uiState: SettingUiState,
    onIntent: (SettingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddAlarmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "알림 설정") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SettingUiIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "alarm_header") {
                    Text(
                        text = "알림 스케줄",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (uiState.isLoading) {
                    item(key = "alarm_loading") {
                        TossWatchLoadingIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                } else if (uiState.configuredAlarms.isEmpty()) {
                    item(key = "alarm_empty") {
                        Text(
                            text = "등록된 알림이 없어요. 종목과 시각을 골라 추가해 보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                } else {
                    items(
                        items = uiState.configuredAlarms,
                        key = { it.id },
                    ) { alarm ->
                        AlarmProfileItem(
                            alarm = alarm,
                            onToggle = { enabled ->
                                onIntent(SettingUiIntent.OnToggleAlarm(alarm.id, enabled))
                            },
                            enabled = !uiState.isSaving,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                item(key = "alarm_add") {
                    OutlinedButton(
                        onClick = { showAddAlarmDialog = true },
                        enabled = !uiState.isSaving && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Text(text = "알림 추가")
                    }
                }

                item(key = "toss_key_section") {
                    TossKeySection(onIntent = onIntent)
                }

                item(key = "watch_section") {
                    WatchTokenSection(onIntent = onIntent)
                }
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(SettingUiIntent.OnErrorDismissed) },
                    title = "설정을 저장하지 못했어요",
                )
            }
        }
    }

    if (showAddAlarmDialog) {
        AddAlarmDialog(
            stocks = uiState.availableStocks,
            onConfirm = { stockCode, hour, minute ->
                showAddAlarmDialog = false
                onIntent(SettingUiIntent.OnAddAlarm(stockCode, hour, minute))
            },
            onDismiss = { showAddAlarmDialog = false },
        )
    }
}

@Composable
private fun TossKeySection(
    onIntent: (SettingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        Text(
            text = "토스 연동",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onIntent(SettingUiIntent.OnTossKeyClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "토스 API 키 재설정")
        }
    }
}

@Composable
private fun WatchTokenSection(
    onIntent: (SettingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 32.dp)) {
        Text(
            text = "Wear OS 연동",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "워치 앱 페어링 화면에 표시된 QR 코드를 스캔해 연동해요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TossWatchButton(
            text = "QR로 워치 연동",
            onClick = { onIntent(SettingUiIntent.OnPairWatchClicked) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingContentPreview() {
    TossWatchTheme {
        SettingContent(
            uiState = SettingUiState(
                configuredAlarms = listOf(
                    AlarmProfile(1L, "005930", "삼성전자", 9, 0, true),
                    AlarmProfile(2L, "035420", "NAVER", 15, 30, false),
                ),
                availableStocks = listOf(
                    CachedStock("005930", "삼성전자"),
                    CachedStock("035420", "NAVER"),
                ),
            ),
            onIntent = {},
        )
    }
}
