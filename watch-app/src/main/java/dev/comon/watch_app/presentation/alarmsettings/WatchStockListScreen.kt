package dev.comon.watch_app.presentation.alarmsettings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.comon.toss_watch.core.model.watch.WatchStock
import dev.comon.watch_app.R

@Composable
fun WatchStockListScreen(state: WatchAlarmUiState, onIntent: (WatchAlarmIntent) -> Unit,
    onBack: () -> Unit, onStockClick: (WatchStock) -> Unit) {
    val snapshot = state.sync.snapshot
    val stocks = snapshot?.stocks.orEmpty()
    // Keep existing alarms accessible even after a stock is no longer held.
    val otherStocks = snapshot?.alarms.orEmpty().filter { alarm -> stocks.none { it.code == alarm.stockCode } }
        .distinctBy { it.stockCode }.map { WatchStock(it.stockCode, it.stockName) }
    AlarmSettingsScaffold(stringResource(R.string.alarm_settings_title)) {
        item { SyncStatus(state) }
        item { SettingsText(stringResource(R.string.alarm_sync_phone_account)) }
        if (stocks.isEmpty() && snapshot?.available == true) {
            item { SettingsText(stringResource(R.string.alarm_no_stocks)) }
        }
        stocks.forEach { stock ->
            item(key = "stock_${stock.code}") {
                val count = snapshot?.alarms.orEmpty().count { it.stockCode == stock.code }
                SettingsButton(stringResource(R.string.alarm_stock_summary, stock.name, count)) { onStockClick(stock) }
            }
        }
        if (otherStocks.isNotEmpty()) item { SettingsText(stringResource(R.string.alarm_other_stocks)) }
        otherStocks.forEach { stock ->
            item(key = "other_${stock.code}") { SettingsButton(stock.name) { onStockClick(stock) } }
        }
        item { SettingsButton(stringResource(R.string.alarm_sync_refresh), !state.submitting) { onIntent(WatchAlarmIntent.Refresh) } }
        item { SettingsButton(stringResource(R.string.alarm_back), onClick = onBack) }
    }
}
