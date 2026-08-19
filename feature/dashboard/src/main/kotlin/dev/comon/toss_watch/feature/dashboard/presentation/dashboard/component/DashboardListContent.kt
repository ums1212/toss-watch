package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.comon.toss_watch.core.designsystem.theme.LocalTossWindowWidth
import dev.comon.toss_watch.core.designsystem.theme.TossAdaptive
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWindowWidth
import dev.comon.toss_watch.core.designsystem.theme.adaptiveContentWidth
import dev.comon.toss_watch.feature.dashboard.R
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiState

/**
 * [dev.comon.toss_watch.feature.dashboard.presentation.dashboard.DashboardScreen]의 리스트 본문 —
 * 풀투리프레시 + 계좌 요약/차트/보유종목 리스트. 창 폭에 따라 배치가 달라진다:
 * - COMPACT/MEDIUM: 요약 카드 → 섹션 헤더+차트 종류 선택기 → 차트 카드 → 보유종목이 세로로 나열된다.
 *   MEDIUM은 [TossAdaptive.holdingColumns]가 2라 보유종목만 2열로 나뉜다.
 * - EXPANDED: 요약 카드와 차트 카드가 한 [Row]에 나란히 배치되고, 보유종목도 2열이다.
 *
 * 전체 콘텐츠는 [dev.comon.toss_watch.core.designsystem.theme.adaptiveContentWidth]로 최대 폭이
 * 제한되고 가로 중앙 정렬된다 — COMPACT에서는 no-op이라 기존 레이아웃과 동일하다.
 *
 * @param bottomContentPadding [DashboardScreen]의 동일 파라미터를 그대로 전달.
 * @param listNestedScrollConnection [DashboardScreen]의 동일 파라미터를 그대로 전달 — 반드시
 *   [PullToRefreshBox]보다 [LazyColumn]에 더 가깝게 붙어야 한다(문서는 BottomMenuScreen 참고).
 */
@Composable
internal fun DashboardListContent(
    uiState: DashboardUiState,
    chartType: PortfolioChartType,
    onChartTypeSelect: (PortfolioChartType) -> Unit,
    onHoldingClick: (stockCode: String, stockName: String) -> Unit,
    onChartClick: () -> Unit,
    onRefresh: () -> Unit,
    bottomContentPadding: Dp,
    listNestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier,
) {
    val securities = uiState.portfolio?.securities.orEmpty()
    val windowWidth = LocalTossWindowWidth.current
    // LazyListScope의 items 빌더 블록은 @Composable 컨텍스트가 아니므로, @Composable getter인
    // TossAdaptive.holdingColumns는 LazyColumn 진입 전 여기서 미리 읽어둔다.
    val columns = TossAdaptive.holdingColumns

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .adaptiveContentWidth(),
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    // PullToRefreshBox의 자체 nestedScroll 커넥션보다 이 LazyColumn에 더 가깝게
                    // 붙어야 풀투리프레시 당김(리스트가 못 움직여 consumed = 0)과 실제 스크롤을
                    // 구분할 수 있다 — 자세한 설명은 BottomMenuScreen 문서 참고.
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(listNestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = TossAdaptive.containerMargin,
                        end = TossAdaptive.containerMargin,
                        top = TossSpacing.stackMd,
                        bottom = TossSpacing.stackMd + bottomContentPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(TossSpacing.stackMd),
                ) {
                    if (windowWidth == TossWindowWidth.EXPANDED && securities.isNotEmpty()) {
                        item(key = "account_and_chart_row") {
                            Row(horizontalArrangement = Arrangement.spacedBy(TossSpacing.stackMd)) {
                                val selectedAccountNo = uiState.accounts
                                    .find { it.accountSeq == uiState.selectedAccountSeq }
                                    ?.accountNo
                                PortfolioSummaryCard(
                                    portfolio = uiState.portfolio,
                                    accountNo = selectedAccountNo,
                                    modifier = Modifier.weight(1f),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    SectionHeader(title = stringResource(id = R.string.dashboard_section_holdings)) {
                                        PortfolioChartTypeSelector(
                                            selected = chartType,
                                            onSelect = onChartTypeSelect,
                                        )
                                    }
                                    PortfolioChartCard(
                                        holdings = securities,
                                        chartType = chartType,
                                        onClick = onChartClick,
                                    )
                                }
                            }
                        }
                    } else {
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
                            SectionHeader(title = stringResource(id = R.string.dashboard_section_holdings)) {
                                if (securities.isNotEmpty()) {
                                    PortfolioChartTypeSelector(
                                        selected = chartType,
                                        onSelect = onChartTypeSelect,
                                    )
                                }
                            }
                        }

                        if (securities.isNotEmpty()) {
                            item(key = "market_performance_chart") {
                                PortfolioChartCard(
                                    holdings = securities,
                                    chartType = chartType,
                                    onClick = onChartClick,
                                )
                            }
                        }
                    }

                    if (securities.isEmpty() && !uiState.isLoading) {
                        item(key = "holding_empty") {
                            Text(
                                text = stringResource(id = R.string.dashboard_holdings_empty),
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
                            items = securities.chunked(columns),
                            key = { chunk -> chunk.first().stockCode },
                        ) { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(TossSpacing.stackMd)) {
                                chunk.forEach { holding ->
                                    HoldingListItem(
                                        holding = holding,
                                        onClick = { onHoldingClick(holding.stockCode, holding.stockName) },
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
    }
}

/** 섹션 제목. [trailingContent]는 제목 오른쪽에 놓이는 선택적 액션 영역(예: 차트 종류 선택기). */
@Composable
private fun SectionHeader(
    title: String,
    trailingContent: @Composable () -> Unit = {},
) {
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
        trailingContent()
    }
}
