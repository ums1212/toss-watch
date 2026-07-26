package dev.comon.toss_watch.feature.dashboard.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
 * @param onNavigateToSetting [DashboardUiSideEffect.NavigateToSetting] 수신 시 호출 —
 *   :app의 Navigation 3 라우터가 SettingRoute push로 연결한다. 보유종목 카드를 탭해 진입한 경우
 *   해당 종목의 stockCode가 전달되며, 설정 아이콘을 통한 진입 시엔 null이다.
 */
@Composable
fun DashboardScreen(
    onNavigateToSetting: (stockCode: String?) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    is DashboardUiSideEffect.NavigateToSetting -> onNavigateToSetting(effect.stockCode)
                }
            }
        }
    }

    DashboardContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onIntent: (DashboardUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isAccountDialogVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                            contentDescription = "알림 설정",
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = TossSpacing.containerMargin,
                        vertical = TossSpacing.stackMd,
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
                                    onIntent(DashboardUiIntent.OnHoldingClicked(holding.stockCode))
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
