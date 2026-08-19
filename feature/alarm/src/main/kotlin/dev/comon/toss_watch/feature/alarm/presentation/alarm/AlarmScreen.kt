package dev.comon.toss_watch.feature.alarm.presentation.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingIndicator
import dev.comon.toss_watch.core.designsystem.theme.TossAdaptive
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.designsystem.theme.adaptiveContentWidth
import dev.comon.toss_watch.core.model.CachedStock
import dev.comon.toss_watch.feature.alarm.R
import dev.comon.toss_watch.feature.alarm.presentation.alarm.component.StockAlarmSummaryItem
import dev.comon.toss_watch.feature.alarm.presentation.alarm.component.StockSelectDialog

/** 알림 목록이 있을 때 FAB가 리스트 마지막 항목을 가리지 않도록 LazyColumn 하단에 더하는 여유 공간. */
private val FAB_CLEARANCE = 72.dp

/**
 * 알림이 등록된 종목 목록 — 종목을 탭하면 해당 종목의 알림 목록(AlarmDetailScreen)으로 이동한다.
 *
 * @param onNavigateToAlarmDetail [AlarmUiSideEffect.NavigateToAlarmDetail] 수신 시 호출.
 * @param bottomContentPadding 이 화면 위에 겹쳐 떠 있는 플로팅 하단 네비게이션 바가 차지하는
 *   높이 — 리스트 마지막 항목이 바에 가려지지 않도록 [LazyColumn]의 하단 contentPadding에 더한다.
 * @param listNestedScrollConnection BottomMenuScreen이 리스트 스크롤 여부를 관찰해 플로팅
 *   하단 바의 슬라이드 아웃/인을 제어하기 위해 전달하는 커넥션.
 */
@Composable
fun AlarmScreen(
    onNavigateToAlarmDetail: (stockCode: String, stockName: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    listNestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {},
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    is AlarmUiSideEffect.NavigateToAlarmDetail ->
                        onNavigateToAlarmDetail(effect.stockCode, effect.stockName)
                }
            }
        }
    }

    AlarmContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
        bottomContentPadding = bottomContentPadding,
        listNestedScrollConnection = listNestedScrollConnection,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmContent(
    uiState: AlarmUiState,
    onIntent: (AlarmUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    listNestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {},
) {
    var showStockSelectDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // 이 화면은 항상 BottomMenuScreen의 Scaffold(bottomBar) 안에 탭 콘텐츠로 호스팅된다 —
        // 시스템 하단 인셋은 그 바깥 Scaffold가 이미 처리하므로, 여기서 기본값(safeDrawing)을
        // 그대로 쓰면 같은 인셋이 두 번 반영되어 탭바와 콘텐츠 사이에 불필요한 여백이 생긴다.
        // 플로팅 하단 바는 콘텐츠 위에 겹쳐 떠 있으므로(하드 클리핑 아님) 그 높이는
        // bottomContentPadding으로 받아 LazyColumn의 contentPadding 및 FAB의 여백에 반영한다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.alarm_tab_title)) })
        },
        floatingActionButton = {
            // 알림이 하나도 없을 때는 빈 상태 안내 아래에 이미 추가 버튼이 있으므로 FAB를 숨겨
            // 진입점이 중복되지 않게 한다.
            if (uiState.stockAlarms.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showStockSelectDialog = true },
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.alarm_add_button),
                    )
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
                    uiState.isLoading && uiState.stockAlarms.isEmpty() -> {
                        TossWatchLoadingIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = TossSpacing.sectionPadding),
                        )
                    }

                    uiState.stockAlarms.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = TossSpacing.containerMargin,
                                    vertical = TossSpacing.sectionPadding,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(id = R.string.alarm_list_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = { showStockSelectDialog = true },
                                modifier = Modifier.padding(top = TossSpacing.containerMargin),
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(TossSpacing.stackSm))
                                Text(text = stringResource(id = R.string.alarm_add_button))
                            }
                        }
                    }

                    else -> {
                        val columns = TossAdaptive.holdingColumns
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(listNestedScrollConnection),
                            contentPadding = PaddingValues(
                                start = TossAdaptive.containerMargin,
                                end = TossAdaptive.containerMargin,
                                top = TossSpacing.stackMd,
                                bottom = TossSpacing.stackMd + bottomContentPadding + FAB_CLEARANCE,
                            ),
                            verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm),
                        ) {
                            items(
                                items = uiState.stockAlarms.chunked(columns),
                                key = { chunk -> chunk.first().stockCode },
                            ) { chunk ->
                                Row(horizontalArrangement = Arrangement.spacedBy(TossSpacing.stackMd)) {
                                    chunk.forEach { summary ->
                                        StockAlarmSummaryItem(
                                            summary = summary,
                                            onClick = {
                                                onIntent(
                                                    AlarmUiIntent.OnStockClicked(
                                                        summary.stockCode,
                                                        summary.stockName,
                                                    ),
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    // 마지막 청크가 columns보다 적으면 빈 칸을 채워 카드 폭이
                                    // 늘어나지 않고 왼쪽 정렬된 것처럼 보이게 한다.
                                    repeat(columns - chunk.size) {
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(AlarmUiIntent.OnErrorDismissed) },
                    title = stringResource(id = R.string.alarm_error_dialog_title),
                )
            }
        }
    }

    if (showStockSelectDialog) {
        StockSelectDialog(
            stocks = uiState.portfolioStocks,
            onStockSelected = { stock ->
                showStockSelectDialog = false
                onIntent(AlarmUiIntent.OnStockClicked(stock.stockCode, stock.stockName))
            },
            onDismiss = { showStockSelectDialog = false },
        )
    }
}

@Preview(name = "Compact", showBackground = true, widthDp = 412)
@Preview(name = "Medium", showBackground = true, widthDp = 700)
@Preview(name = "Expanded", showBackground = true, widthDp = 1200, heightDp = 800)
@Composable
private fun AlarmContentPreview() {
    TossWatchTheme {
        AlarmContent(
            uiState = AlarmUiState(
                stockAlarms = listOf(
                    StockAlarmSummary(stockCode = "005930", stockName = "삼성전자", alarmCount = 2),
                    StockAlarmSummary(stockCode = "035420", stockName = "NAVER", alarmCount = 1),
                ),
                portfolioStocks = listOf(
                    CachedStock(stockCode = "005930", stockName = "삼성전자"),
                    CachedStock(stockCode = "035420", stockName = "NAVER"),
                    CachedStock(stockCode = "000660", stockName = "SK하이닉스"),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmContentEmptyPreview() {
    TossWatchTheme {
        AlarmContent(
            uiState = AlarmUiState(),
            onIntent = {},
        )
    }
}
