package dev.comon.watch_app.domain.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.watch_app.domain.model.StockQuote

interface WatchStockQuoteRepository {
    suspend fun fetch(stockCode: String): NetworkResult<StockQuote>
}
