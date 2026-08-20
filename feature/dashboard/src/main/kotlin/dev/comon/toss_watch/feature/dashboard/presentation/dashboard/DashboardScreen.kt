package dev.comon.toss_watch.feature.dashboard.presentation.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.res.stringResource
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
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.R
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.domain.model.Currency
import dev.comon.toss_watch.feature.dashboard.domain.model.HoldingStock
import dev.comon.toss_watch.feature.dashboard.domain.model.Portfolio
import dev.comon.toss_watch.feature.dashboard.domain.model.PortfolioSummary
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiIntent
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiSideEffect
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiState
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardViewModel
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.DashboardListContent
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.PortfolioChartFullScreenDialog
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.PortfolioChartType

/**
 * 자산/보유 종목 대시보드.
 *
 * 상단 앱바는 이 화면이 아니라 [dev.comon.toss_watch.navigation.BottomMenuScreen]이
 * [dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.DashboardTopBar]로
 * 소유한다 — 탭을 전환해도 탑바가 화면 최상단에 고정되도록 하기 위함이다(CLAUDE.md 참고).
 *
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

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onIntent: (DashboardUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    listNestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {},
) {
    var isChartDialogVisible by rememberSaveable { mutableStateOf(false) }
    var chartType by rememberSaveable { mutableStateOf(PortfolioChartType.BUBBLE) }

    // 상단 앱바는 BottomMenuScreen의 DashboardTopBar가 소유하므로 여기서는 별도 Scaffold 없이
    // 콘텐츠만 채운다. 이 화면은 항상 BottomMenuScreen의 Scaffold(bottomBar) 안에 탭 콘텐츠로
    // 호스팅되므로 시스템 인셋도 그 바깥 Scaffold가 이미 처리한다 — 플로팅 하단 바가 콘텐츠 위에
    // 겹쳐 떠 있는 높이만 bottomContentPadding으로 받아 LazyColumn의 contentPadding에 반영한다.
    Box(modifier = modifier.fillMaxSize()) {
        val securities = uiState.portfolio?.securities.orEmpty()

        DashboardListContent(
            uiState = uiState,
            chartType = chartType,
            onChartTypeSelect = { chartType = it },
            onHoldingClick = { stockCode, stockName ->
                onIntent(DashboardUiIntent.OnHoldingClicked(stockCode = stockCode, stockName = stockName))
            },
            onChartClick = { isChartDialogVisible = true },
            onRefresh = { onIntent(DashboardUiIntent.OnRefreshTriggered) },
            bottomContentPadding = bottomContentPadding,
            listNestedScrollConnection = listNestedScrollConnection,
        )

        if (uiState.isLoading) {
            TossWatchLoadingOverlay(message = stringResource(id = R.string.dashboard_loading_message))
        }

        uiState.errorMessage?.let { message ->
            TossWatchErrorDialog(
                message = message,
                onDismiss = { onIntent(DashboardUiIntent.OnErrorDismissed) },
                title = stringResource(id = R.string.dashboard_error_dialog_title),
            )
        }

        if (isChartDialogVisible && securities.isNotEmpty()) {
            PortfolioChartFullScreenDialog(
                holdings = securities,
                chartType = chartType,
                onDismiss = { isChartDialogVisible = false },
            )
        }
    }
}

@Preview(name = "Compact", showBackground = true, widthDp = 412)
@Preview(name = "Medium", showBackground = true, widthDp = 700)
@Preview(name = "Expanded", showBackground = true, widthDp = 1200, heightDp = 800)
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
                    exchangeRate = 1_380.5,
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
