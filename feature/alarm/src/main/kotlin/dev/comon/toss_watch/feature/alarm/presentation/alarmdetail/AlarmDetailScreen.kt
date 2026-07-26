package dev.comon.toss_watch.feature.alarm.presentation.alarmdetail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingIndicator
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.presentation.component.AddAlarmDialog
import dev.comon.toss_watch.feature.alarm.presentation.component.AlarmProfileItem

/**
 * 종목별 알림 목록 — 등록/토글/삭제. 알림 추가는 하단 버튼으로만 진입한다.
 *
 * @param stockCode 대시보드 보유종목 카드 또는 알림 탭의 종목 항목을 탭해 진입할 때 전달되는 종목 코드.
 * @param stockName 진입 시점에 함께 전달되는 종목명 — 상단 타이틀 및 알림 추가 다이얼로그에 쓰인다.
 *   비어 있으면 이미 등록된 알림에서 종목명을 대체해 찾고, 그마저 없으면 종목 코드로 대체한다.
 * @param onNavigateBack [AlarmDetailUiSideEffect.NavigateBack] 수신 시 호출.
 */
@Composable
fun AlarmDetailScreen(
    stockCode: String,
    stockName: String?,
    onNavigateBack: () -> Unit,
    viewModel: AlarmDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    AlarmDetailUiSideEffect.NavigateBack -> onNavigateBack()
                    is AlarmDetailUiSideEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlarmDetailContent(
        stockCode = stockCode,
        stockName = stockName,
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onBackClicked = { viewModel.handleIntent(AlarmDetailUiIntent.OnBackClicked) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmDetailContent(
    stockCode: String,
    stockName: String?,
    uiState: AlarmDetailUiState,
    onIntent: (AlarmDetailUiIntent) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stockAlarms = uiState.alarms.filter { it.stockCode == stockCode }
    val resolvedStockName = stockName ?: stockAlarms.firstOrNull()?.stockName ?: stockCode

    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var alarmPendingDelete by remember { mutableStateOf<AlarmProfile?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = resolvedStockName) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
        bottomBar = {
            OutlinedButton(
                onClick = { showAddAlarmDialog = true },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TossSpacing.containerMargin),
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Text(text = "알림 추가")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading && stockAlarms.isEmpty() -> {
                    TossWatchLoadingIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = TossSpacing.sectionPadding),
                    )
                }

                stockAlarms.isEmpty() -> {
                    Text(
                        text = "등록된 알림이 없어요. 아래 버튼으로 추가해 보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = TossSpacing.containerMargin,
                                vertical = TossSpacing.sectionPadding,
                            ),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = TossSpacing.containerMargin,
                            vertical = TossSpacing.stackMd,
                        ),
                        verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm),
                    ) {
                        items(
                            items = stockAlarms,
                            key = { it.id },
                        ) { alarm ->
                            AlarmProfileItem(
                                alarm = alarm,
                                onToggle = { enabled ->
                                    onIntent(AlarmDetailUiIntent.OnToggleAlarm(alarm.id, enabled))
                                },
                                onDelete = { alarmPendingDelete = alarm },
                                enabled = !uiState.isSaving,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(AlarmDetailUiIntent.OnErrorDismissed) },
                    title = "설정을 저장하지 못했어요",
                )
            }
        }
    }

    if (showAddAlarmDialog) {
        AddAlarmDialog(
            stocks = emptyList(),
            lockedStock = CachedStock(stockCode = stockCode, stockName = resolvedStockName),
            onConfirm = { code, hour, minute, daysOfWeek ->
                showAddAlarmDialog = false
                onIntent(AlarmDetailUiIntent.OnAddAlarm(code, hour, minute, daysOfWeek))
            },
            onDismiss = { showAddAlarmDialog = false },
        )
    }

    alarmPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { alarmPendingDelete = null },
            title = { Text(text = "알림 삭제") },
            text = { Text(text = "'${target.stockName}' 알림을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(AlarmDetailUiIntent.OnDeleteAlarm(target.id))
                        alarmPendingDelete = null
                    },
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmPendingDelete = null }) {
                    Text(text = "취소")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmDetailContentPreview() {
    TossWatchTheme {
        AlarmDetailContent(
            stockCode = "005930",
            stockName = "삼성전자",
            uiState = AlarmDetailUiState(
                alarms = listOf(
                    AlarmProfile(1L, "005930", "삼성전자", 9, 0, listOf(0, 1, 2, 3, 4, 5, 6), true),
                    AlarmProfile(2L, "005930", "삼성전자", 15, 30, listOf(0, 1, 2, 3, 4), false),
                ),
            ),
            onIntent = {},
            onBackClicked = {},
        )
    }
}
