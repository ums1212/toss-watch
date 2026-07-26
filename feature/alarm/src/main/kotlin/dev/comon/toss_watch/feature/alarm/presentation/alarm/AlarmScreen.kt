package dev.comon.toss_watch.feature.alarm.presentation.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import dev.comon.toss_watch.feature.alarm.presentation.alarm.component.StockAlarmSummaryItem

/**
 * 알림이 등록된 종목 목록 — 종목을 탭하면 해당 종목의 알림 목록(AlarmDetailScreen)으로 이동한다.
 *
 * @param onNavigateToAlarmDetail [AlarmUiSideEffect.NavigateToAlarmDetail] 수신 시 호출.
 */
@Composable
fun AlarmScreen(
    onNavigateToAlarmDetail: (stockCode: String, stockName: String) -> Unit,
    modifier: Modifier = Modifier,
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmContent(
    uiState: AlarmUiState,
    onIntent: (AlarmUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // 이 화면은 항상 BottomMenuScreen의 Scaffold(bottomBar) 안에 탭 콘텐츠로 호스팅된다 —
        // 시스템 하단 인셋은 그 바깥 Scaffold가 이미 처리하므로, 여기서 기본값(safeDrawing)을
        // 그대로 쓰면 같은 인셋이 두 번 반영되어 탭바와 콘텐츠 사이에 불필요한 여백이 생긴다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text(text = "알림") })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                    Text(
                        text = "등록된 알림이 없어요.\n보유 종목 카드를 눌러 알림을 추가해 보세요.",
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
                            items = uiState.stockAlarms,
                            key = { it.stockCode },
                        ) { summary ->
                            StockAlarmSummaryItem(
                                summary = summary,
                                onClick = {
                                    onIntent(AlarmUiIntent.OnStockClicked(summary.stockCode, summary.stockName))
                                },
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(AlarmUiIntent.OnErrorDismissed) },
                    title = "불러오기에 실패했어요",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmContentPreview() {
    TossWatchTheme {
        AlarmContent(
            uiState = AlarmUiState(
                stockAlarms = listOf(
                    StockAlarmSummary(stockCode = "005930", stockName = "삼성전자", alarmCount = 2),
                    StockAlarmSummary(stockCode = "035420", stockName = "NAVER", alarmCount = 1),
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
