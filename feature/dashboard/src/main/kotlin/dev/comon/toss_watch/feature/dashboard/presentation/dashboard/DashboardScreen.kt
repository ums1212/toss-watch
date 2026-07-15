package dev.comon.toss_watch.feature.dashboard.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingOverlay
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
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.PortfolioSummaryCard

/**
 * 자산/보유 종목 대시보드.
 *
 * @param onNavigateToSetting [DashboardUiSideEffect.NavigateToSetting] 수신 시 호출 —
 *   :app의 Navigation 3 라우터가 SettingRoute push로 연결한다.
 */
@Composable
fun DashboardScreen(
    onNavigateToSetting: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    DashboardUiSideEffect.NavigateToSetting -> onNavigateToSetting()
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(key = "portfolio_summary") {
                        PortfolioSummaryCard(
                            portfolio = uiState.portfolio,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

                    item(key = "holding_header") {
                        Text(
                            text = "보유 종목",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    val securities = uiState.portfolio?.securities.orEmpty()
                    if (securities.isEmpty() && !uiState.isLoading) {
                        item(key = "holding_empty") {
                            Text(
                                text = "보유 중인 종목이 없어요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                            )
                        }
                    } else {
                        items(
                            items = securities,
                            key = { it.stockCode },
                        ) { holding ->
                            HoldingListItem(holding = holding)
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
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
                        totalInvestmentKrw = 650_000.0,
                        totalInvestmentUsd = 0.0,
                        totalEvaluationKrw = 725_000.0,
                        totalEvaluationUsd = 0.0,
                        totalProfitLossKrw = 75_000.0,
                        totalProfitLossUsd = 0.0,
                        totalReturnRate = 11.54,
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
