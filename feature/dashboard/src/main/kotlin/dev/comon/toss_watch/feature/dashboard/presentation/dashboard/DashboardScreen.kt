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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingOverlay
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.TargetTicker
import dev.comon.toss_watch.feature.dashboard.domain.model.UserAssets
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiIntent
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiSideEffect
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiState
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardViewModel
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.AssetSummaryCard
import dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component.TickerListItem

/**
 * 자산/관찰 종목 대시보드.
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "내 자산") },
                actions = {
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
                    item(key = "asset_summary") {
                        AssetSummaryCard(
                            userAssets = uiState.totalAssets,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }

                    item(key = "ticker_header") {
                        Text(
                            text = "관찰 중인 종목",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    if (uiState.activeTickers.isEmpty() && !uiState.isLoading) {
                        item(key = "ticker_empty") {
                            Text(
                                text = "아직 관찰 중인 종목이 없어요.\n설정에서 알림 종목을 추가해 보세요.",
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
                            items = uiState.activeTickers,
                            key = { it.code },
                        ) { ticker ->
                            TickerListItem(ticker = ticker)
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    TossWatchTheme {
        DashboardContent(
            uiState = DashboardUiState(
                totalAssets = UserAssets(12_345_678L, 345_678L, 2.88),
                activeTickers = listOf(
                    TargetTicker("005930", "삼성전자", 78_400L, 1.42),
                    TargetTicker("035420", "NAVER", 187_500L, -0.83),
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
