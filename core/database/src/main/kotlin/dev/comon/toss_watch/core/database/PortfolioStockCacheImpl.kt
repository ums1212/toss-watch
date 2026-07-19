package dev.comon.toss_watch.core.database

import dev.comon.toss_watch.core.database.dao.PortfolioStockDao
import dev.comon.toss_watch.core.database.entity.PortfolioStockEntity
import dev.comon.toss_watch.core.model.CachedStock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PortfolioStockCacheImpl @Inject constructor(
    private val portfolioStockDao: PortfolioStockDao,
) : PortfolioStockCache {

    override fun observeStocks(): Flow<List<CachedStock>> =
        portfolioStockDao.observeAll().map { entities -> entities.map { it.toCachedStock() } }

    override suspend fun replaceStocks(stocks: List<CachedStock>) {
        portfolioStockDao.replaceAll(stocks.map { it.toEntity() })
    }

    private fun PortfolioStockEntity.toCachedStock(): CachedStock =
        CachedStock(stockCode = stockCode, stockName = stockName)

    private fun CachedStock.toEntity(): PortfolioStockEntity =
        PortfolioStockEntity(stockCode = stockCode, stockName = stockName)
}
