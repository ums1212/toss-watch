package dev.comon.toss_watch.feature.dashboard.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingOverlay
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.PortfolioSummary
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiIntent
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiSideEffect
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiState
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardViewModel
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.AccountSelectDialog
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.HoldingListItem
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.PortfolioBubbleChart
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.PortfolioSummaryCard

/**
 * 자산/보유 종목 대시보드.
 *
 * @param onNavigateToSetting [DashboardUiSideEffect.NavigateToSetting] 수신 시 호출 — 상단 앱바의 설정 아이콘.
 * @param onNavigateToAlarmDetail [DashboardUiSideEffect.NavigateToAlarmDetail] 수신 시 호출 —
 *   보유종목 카드를 탭해 진입, 해당 종목의 알림 목록(AlarmDetailScreen)으로 이동한다.
 * @param bottomContentPadding 이 화면 위에 겹쳐 떠 있는 플로팅 하단 네비게이션 바가 차지하는
 *   높이 — 리스트 마지막 항목이 바에 가려지지 않도록 [LazyColumn]의 하단 contentPadding에 더한다.
 * @param listNestedScrollConnection [BottomMenuScreen]이 리스트 스크롤 여부를 관찰해 플로팅
 *   하단 바의 슬라이드 아웃/인을 제어하기 위해 전달하는 커넥션. `PullToRefreshBox`보다 반드시
 *   [LazyColumn]에 더 가깝게(먼저) 붙어야 풀투리프레시 당김과 실제 스크롤을 구분할 수 있다.
 */
@Composable
fun DashboardScreen(
    onNavigateToSetting: () -> Unit,
    onNavigateToAlarmDetail: (stockCode: String, stockName: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    listNestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    DashboardUiSideEffect.NavigateToSetting -> onNavigateToSetting()
                    is DashboardUiSideEffect.NavigateToAlarmDetail ->
                        onNavigateToAlarmDetail(effect.stockCode, effect.stockName)
                }
            }
        }
    }

    DashboardContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
        bottomContentPadding = bottomContentPadding,
        listNestedScrollConnection = listNestedScrollConnection,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onIntent: (DashboardUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    listNestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {},
) {
    var isAccountDialogVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // 이 화면은 항상 BottomMenuScreen의 Scaffold(bottomBar) 안에 탭 콘텐츠로 호스팅된다 —
        // 시스템 하단 인셋은 그 바깥 Scaffold가 이미 처리하므로, 여기서 기본값(safeDrawing)을
        // 그대로 쓰면 같은 인셋이 두 번 반영되어 탭바와 콘텐츠 사이에 불필요한 여백이 생긴다.
        // 플로팅 하단 바는 콘텐츠 위에 겹쳐 떠 있으므로(하드 클리핑 아님) 그 높이는
        // bottomContentPadding으로 받아 LazyColumn의 contentPadding에 반영한다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = "내 자산") },
                actions = {
                    IconButton(
                        onClick = { isAccountDialogVisible = true },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "계좌 목록",
                        )
                    }
                    IconButton(
                        onClick = { onIntent(DashboardUiIntent.OnSettingClicked) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "설정",
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
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onIntent(DashboardUiIntent.OnRefreshTriggered) },
                modifier = Modifier.fillMaxSize(),
            ) {
                val securities = uiState.portfolio?.securities.orEmpty()

                LazyColumn(
                    // PullToRefreshBox의 자체 nestedScroll 커넥션보다 이 LazyColumn에 더 가깝게
                    // 붙어야 풀투리프레시 당김(리스트가 못 움직여 consumed = 0)과 실제 스크롤을
                    // 구분할 수 있다 — 자세한 설명은 BottomMenuScreen 문서 참고.
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(listNestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = TossSpacing.containerMargin,
                        end = TossSpacing.containerMargin,
                        top = TossSpacing.stackMd,
                        bottom = TossSpacing.stackMd + bottomContentPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(TossSpacing.stackMd),
                ) {
                    item(key = "account_card") {
                        val selectedAccountNo = uiState.accounts
                            .find { it.accountSeq == uiState.selectedAccountSeq }
                            ?.accountNo
                        PortfolioSummaryCard(
                            portfolio = uiState.portfolio,
                            accountNo = selectedAccountNo,
                        )
                    }

                    item(key = "market_performance_header") {
                        SectionHeader(title = "보유 종목")
                    }

                    if (securities.isNotEmpty()) {
                        item(key = "market_performance_chart") {
                            PortfolioBubbleChart(holdings = securities)
                        }
                    }

                    if (securities.isEmpty() && !uiState.isLoading) {
                        item(key = "holding_empty") {
                            Text(
                                text = "보유 중인 종목이 없어요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = TossSpacing.sectionPadding),
                            )
                        }
                    } else {
                        items(
                            items = securities,
                            key = { it.stockCode },
                        ) { holding ->
                            HoldingListItem(
                                holding = holding,
                                onClick = {
                                    onIntent(
                                        DashboardUiIntent.OnHoldingClicked(
                                            stockCode = holding.stockCode,
                                            stockName = holding.stockName,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                TossWatchLoadingOverlay(message = "자산 정보를 불러오는 중이에요…")
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(DashboardUiIntent.OnErrorDismissed) },
                    title = "불러오기에 실패했어요",
                )
            }

            if (isAccountDialogVisible) {
                AccountSelectDialog(
                    accounts = uiState.accounts,
                    selectedAccountSeq = uiState.selectedAccountSeq,
                    onSelect = { accountSeq ->
                        onIntent(DashboardUiIntent.OnAccountSelected(accountSeq))
                        isAccountDialogVisible = false
                    },
                    onDismiss = { isAccountDialogVisible = false },
                )
            }
        }
    }
}

/** 섹션 제목. */
@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TossSpacing.stackSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    TossWatchTheme {
        DashboardContent(
            uiState = DashboardUiState(
                accounts = listOf(
                    Account(accountNo = "100012345678", accountSeq = 987654, accountType = "BROKERAGE"),
                ),
                selectedAccountSeq = 987654,
                portfolio = Portfolio(
                    summary = PortfolioSummary(
                        totalInvestmentKrw = 5_400_000.0,
                        totalInvestmentUsd = 6_200.0,
                        totalEvaluationKrw = 6_050_000.0,
                        totalEvaluationUsd = 6_640.0,
                        totalProfitLossKrw = 650_000.0,
                        totalProfitLossUsd = 440.0,
                        totalReturnRate = 12.04,
                    ),
                    securities = listOf(
                        HoldingStock(
                            stockCode = "005930",
                            stockName = "삼성전자",
                            currency = Currency.KRW,
                            quantity = 10.0,
                            averageBuyPrice = 65_000.0,
                            totalBuyAmount = 650_000.0,
                            currentPrice = 72_500.0,
                            totalEvaluationAmount = 725_000.0,
                            profitLoss = 75_000.0,
                            returnRate = 11.54,
                        ),
                        HoldingStock(
                            stockCode = "NVDA",
                            stockName = "Nvidia Corp",
                            currency = Currency.USD,
                            quantity = 10.0,
                            averageBuyPrice = 700.0,
                            totalBuyAmount = 7_000.0,
                            currentPrice = 875.28,
                            totalEvaluationAmount = 8_752.8,
                            profitLoss = 1_752.8,
                            returnRate = 5.7,
                        ),
                        HoldingStock(
                            stockCode = "TSLA",
                            stockName = "Tesla Inc",
                            currency = Currency.USD,
                            quantity = 20.0,
                            averageBuyPrice = 180.0,
                            totalBuyAmount = 3_600.0,
                            currentPrice = 175.40,
                            totalEvaluationAmount = 3_508.0,
                            profitLoss = -92.0,
                            returnRate = -3.1,
                        ),
                        HoldingStock(
                            stockCode = "AAPL",
                            stockName = "Apple Inc",
                            currency = Currency.USD,
                            quantity = 15.0,
                            averageBuyPrice = 178.0,
                            totalBuyAmount = 2_670.0,
                            currentPrice = 182.52,
                            totalEvaluationAmount = 2_737.8,
                            profitLoss = 67.8,
                            returnRate = 2.4,
                        ),
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentEmptyPreview() {
    TossWatchTheme {
        DashboardContent(
            uiState = DashboardUiState(),
            onIntent = {},
        )
    }
}
