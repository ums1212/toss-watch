package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import dev.comon.toss_watch.core.model.watch.WatchStock
import dev.comon.watch_app.R

/** 페이저의 종목 한 장. [held]가 false면 보유하지 않았지만 알람이 남아 있는 종목이다. */
internal data class WatchStockPage(val stock: WatchStock, val alarmCount: Int, val held: Boolean)

/**
 * 첫 페이지는 동기화 상태·새로고침·설정을 모은 개요 페이지이고, 이후 페이지마다 종목 하나를
 * 크게 보여준다. 가로로 한 장씩 넘겨 작은 화면에서도 종목명을 읽기 쉽게 한다.
 */
@Composable
fun WatchStockListScreen(state: WatchAlarmUiState, onIntent: (WatchAlarmIntent) -> Unit,
    onSettingsClick: () -> Unit, onStockClick: (WatchStock) -> Unit) {
    val snapshot = state.sync.snapshot
    val alarms = snapshot?.alarms.orEmpty()
    val stocks = snapshot?.stocks.orEmpty()
    // Keep existing alarms accessible even after a stock is no longer held.
    val otherStocks = alarms.filter { alarm -> stocks.none { it.code == alarm.stockCode } }
        .distinctBy { it.stockCode }.map { WatchStock(it.stockCode, it.stockName) }
    val stockPages = stocks.map { stock -> WatchStockPage(stock, alarms.count { it.stockCode == stock.code }, held = true) } +
        otherStocks.map { stock -> WatchStockPage(stock, alarms.count { it.stockCode == stock.code }, held = false) }
    // pageCount 람다는 PagerState 생성 시 한 번 캡처되므로, 동기화로 바뀐 최신 목록을 읽도록 한다.
    val latestPages by rememberUpdatedState(stockPages)
    val pagerState = rememberPagerState { 1 + latestPages.size }

    AppScaffold {
        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(
                state = pagerState,
                key = { page -> if (page == 0) "overview" else latestPages.getOrNull(page - 1)?.stock?.code ?: "page_$page" },
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    val stockPage = stockPages.getOrNull(page - 1)
                    if (page == 0 || stockPage == null) {
                        OverviewPage(state, hasStocks = stockPages.isNotEmpty(), onIntent, onSettingsClick)
                    } else {
                        WatchStockPageContent(stockPage) { onStockClick(stockPage.stock) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewPage(state: WatchAlarmUiState, hasStocks: Boolean,
    onIntent: (WatchAlarmIntent) -> Unit, onSettingsClick: () -> Unit) {
    val snapshot = state.sync.snapshot
    AlarmSettingsList(stringResource(R.string.alarm_settings_title)) {
        item { SyncStatus(state) }
        item { SettingsText(stringResource(R.string.alarm_sync_phone_account)) }
        if (hasStocks) {
            item { SettingsText(stringResource(R.string.alarm_swipe_hint)) }
        } else if (snapshot?.available == true) {
            item { SettingsText(stringResource(R.string.alarm_no_stocks)) }
        }
        item { SettingsButton(stringResource(R.string.alarm_sync_refresh), !state.submitting) { onIntent(WatchAlarmIntent.Refresh) } }
        item { SettingsButton(stringResource(R.string.watch_settings_title), onClick = onSettingsClick) }
    }
}
