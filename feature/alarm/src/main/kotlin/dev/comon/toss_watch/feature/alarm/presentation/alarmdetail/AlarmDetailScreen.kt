package dev.comon.toss_watch.feature.alarm.presentation.alarmdetail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import dev.comon.toss_watch.core.designsystem.theme.adaptiveContentWidth
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.alarm.R
import dev.comon.toss_watch.feature.alarm.domain.model.AlarmProfile
import dev.comon.toss_watch.feature.alarm.presentation.component.AddAlarmDialog
import dev.comon.toss_watch.feature.alarm.presentation.component.AlarmProfileItem
import dev.comon.toss_watch.feature.alarm.presentation.component.SwipeToDeleteBox
import dev.comon.toss_watch.feature.alarm.presentation.component.formatDaysOfWeek

/**
 * 종목별 알림 목록 — 등록/토글/삭제. 알림 추가는 하단 버튼으로만 진입한다.
 *
 * @param stockCode 대시보드 보유종목 카드 또는 알림 탭의 종목 항목을 탭해 진입할 때 전달되는 종목 코드.
 * @param stockName 진입 시점에 함께 전달되는 종목명 — 상단 타이틀 및 알림 추가 다이얼로그에 쓰인다.
 *   비어 있으면 이미 등록된 알림에서 종목명을 대체해 찾고, 그마저 없으면 종목 코드로 대체한다.
 * @param onNavigateBack [AlarmDetailUiSideEffect.NavigateBack] 수신 시 호출.
 * @param showBackButton 상단 앱바에 뒤로가기 아이콘을 표시할지 여부. 기본값 true(전체화면 오버레이로
 *   진입하는 일반 경로). EXPANDED 창 폭의 List-Detail 2-pane에서 detail pane으로 재사용될 때는
 *   false로 전달돼, 다른 종목 선택이 곧 "이동"인 pane UI에서 불필요한 뒤로가기 아이콘을 숨긴다.
 *   체크 모드(다중 선택) 종료용 닫기 아이콘은 이 값과 무관하게 항상 노출된다.
 */
@Composable
fun AlarmDetailScreen(
    stockCode: String,
    stockName: String?,
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
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
        showBackButton = showBackButton,
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
    showBackButton: Boolean = true,
) {
    val stockAlarms = uiState.alarms.filter { it.stockCode == stockCode }
    val resolvedStockName = stockName ?: stockAlarms.firstOrNull()?.stockName ?: stockCode

    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var alarmPendingDelete by remember { mutableStateOf<AlarmProfile?>(null) }
    var isCheckMode by remember { mutableStateOf(false) }
    var selectedAlarmIds by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    fun exitCheckMode() {
        isCheckMode = false
        selectedAlarmIds = emptySet()
    }

    BackHandler(enabled = isCheckMode) { exitCheckMode() }

    // Scaffold의 contentWindowInsets는 body(innerPadding)에만 적용되고 bottomBar 슬롯에는
    // 적용되지 않는다 — NavigationBar 같은 M3 컴포넌트와 달리 순수 버튼은 인셋을 스스로
    // 소비해야 3버튼 네비게이션바 환경(제스처 미사용)에서 가려지지 않는다.
    // BottomMenuScreen의 FloatingBottomNavigationBar와 동일한 처리.
    val bottomButtonModifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(TossSpacing.containerMargin)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = resolvedStockName) },
                navigationIcon = {
                    // 체크 모드 종료 아이콘은 pane/전체화면 여부와 무관하게 항상 노출한다 —
                    // showBackButton은 "일반 뒤로가기"만 pane 모드에서 숨기기 위한 옵션이다.
                    if (showBackButton || isCheckMode) {
                        IconButton(onClick = { if (isCheckMode) exitCheckMode() else onBackClicked() }) {
                            Icon(
                                imageVector = if (isCheckMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isCheckMode) {
                                    stringResource(id = R.string.alarm_detail_check_mode_close_desc)
                                } else {
                                    stringResource(id = R.string.alarm_detail_back_desc)
                                },
                            )
                        }
                    }
                },
                actions = {
                    if (isCheckMode) {
                        val allSelected = stockAlarms.isNotEmpty() &&
                            selectedAlarmIds.size == stockAlarms.size
                        TextButton(
                            onClick = {
                                selectedAlarmIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    stockAlarms.map { it.id }.toSet()
                                }
                            },
                        ) {
                            Text(
                                text = if (allSelected) {
                                    stringResource(id = R.string.alarm_detail_deselect_all)
                                } else {
                                    stringResource(id = R.string.alarm_detail_select_all)
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isCheckMode) {
                OutlinedButton(
                    onClick = { showDeleteSelectedDialog = true },
                    enabled = !uiState.isSaving && selectedAlarmIds.isNotEmpty(),
                    modifier = bottomButtonModifier,
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                    Text(
                        text = stringResource(
                            id = R.string.alarm_detail_delete_selected_button,
                            selectedAlarmIds.size,
                        ),
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { showAddAlarmDialog = true },
                    enabled = !uiState.isSaving,
                    modifier = bottomButtonModifier,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Text(text = stringResource(id = R.string.alarm_detail_add_button))
                }
            }
        },
    ) { innerPadding ->
        // 태블릿/대화면에서 리스트가 화면 전체 폭으로 늘어지지 않도록 콘텐츠 폭을 제한하고
        // 가로 중앙에 정렬한다 — COMPACT에서는 adaptiveContentWidth()가 no-op이라 기존과 동일하다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .adaptiveContentWidth(),
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
                            text = stringResource(id = R.string.alarm_detail_empty),
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
                                val isSelected = alarm.id in selectedAlarmIds
                                SwipeToDeleteBox(
                                    onDelete = { alarmPendingDelete = alarm },
                                    enabled = !uiState.isSaving && !isCheckMode,
                                ) {
                                    AlarmProfileItem(
                                        alarm = alarm,
                                        onToggle = { enabled ->
                                            onIntent(AlarmDetailUiIntent.OnToggleAlarm(alarm.id, enabled))
                                        },
                                        onDelete = { alarmPendingDelete = alarm },
                                        enabled = !uiState.isSaving,
                                        isCheckMode = isCheckMode,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isCheckMode) {
                                                selectedAlarmIds = if (isSelected) {
                                                    selectedAlarmIds - alarm.id
                                                } else {
                                                    selectedAlarmIds + alarm.id
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (!isCheckMode) {
                                                isCheckMode = true
                                                selectedAlarmIds = setOf(alarm.id)
                                            }
                                        },
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(AlarmDetailUiIntent.OnErrorDismissed) },
                    title = stringResource(id = R.string.alarm_detail_error_dialog_title),
                )
            }
        }
    }

    if (showAddAlarmDialog) {
        AddAlarmDialog(
            stocks = emptyList(),
            lockedStock = CachedStock(stockCode = stockCode, stockName = resolvedStockName),
            onConfirm = { code, name, hour, minute, daysOfWeek ->
                showAddAlarmDialog = false
                onIntent(AlarmDetailUiIntent.OnAddAlarm(code, name, hour, minute, daysOfWeek))
            },
            onDismiss = { showAddAlarmDialog = false },
        )
    }

    alarmPendingDelete?.let { target ->
        val scheduleLabel = "${formatDaysOfWeek(target.daysOfWeek)} " +
            "%02d:%02d".format(target.hour, target.minute)
        AlertDialog(
            onDismissRequest = { alarmPendingDelete = null },
            title = { Text(text = stringResource(id = R.string.alarm_delete_dialog_title)) },
            text = {
                Text(text = stringResource(id = R.string.alarm_delete_dialog_message, scheduleLabel))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(AlarmDetailUiIntent.OnDeleteAlarm(target.id))
                        alarmPendingDelete = null
                    },
                ) {
                    Text(text = stringResource(id = R.string.alarm_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmPendingDelete = null }) {
                    Text(text = stringResource(id = R.string.alarm_delete_cancel))
                }
            },
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(text = stringResource(id = R.string.alarm_delete_selected_dialog_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.alarm_delete_selected_dialog_message,
                        selectedAlarmIds.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(AlarmDetailUiIntent.OnDeleteAlarms(selectedAlarmIds.toList()))
                        showDeleteSelectedDialog = false
                        exitCheckMode()
                    },
                ) {
                    Text(text = stringResource(id = R.string.alarm_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(text = stringResource(id = R.string.alarm_delete_cancel))
                }
            },
        )
    }
}

@Preview(name = "Compact", showBackground = true, widthDp = 412)
@Preview(name = "Medium", showBackground = true, widthDp = 700)
@Preview(name = "Expanded", showBackground = true, widthDp = 1200, heightDp = 800)
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
