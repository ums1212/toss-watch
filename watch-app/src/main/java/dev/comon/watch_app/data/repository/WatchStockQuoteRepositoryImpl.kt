package dev.comon.watch_app.data.repository

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.map
import dev.comon.watch_app.data.local.PairingPreferences
import dev.comon.watch_app.data.remote.WatchApi
import dev.comon.watch_app.data.remote.dto.StockQuoteRequest
import dev.comon.watch_app.data.remote.dto.toDomain
import dev.comon.watch_app.data.remote.watchSafeApiCall
import dev.comon.watch_app.domain.model.StockQuote
import dev.comon.watch_app.domain.repository.WatchStockQuoteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchStockQuoteRepositoryImpl @Inject constructor(
    private val api: WatchApi,
    private val pairing: PairingPreferences,
) : WatchStockQuoteRepository {

    override suspend fun fetch(stockCode: String): NetworkResult<StockQuote> {
        val uuid = pairing.getOrCreateDeviceUuid()
        return watchSafeApiCall { api.getStockQuote(StockQuoteRequest(uuid = uuid, stockCode = stockCode)) }
            .map { it.toDomain() }
    }
}
