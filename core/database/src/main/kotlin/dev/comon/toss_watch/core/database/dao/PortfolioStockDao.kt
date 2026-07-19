package dev.comon.toss_watch.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.comon.toss_watch.core.database.entity.PortfolioStockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioStockDao {

    @Query("SELECT * FROM portfolio_stocks")
    fun observeAll(): Flow<List<PortfolioStockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<PortfolioStockEntity>)

    @Query("DELETE FROM portfolio_stocks")
    suspend fun deleteAll()

    /** 현재 캐시를 [stocks]로 통째로 교체한다 — 대시보드가 새 포트폴리오를 받을 때마다 호출. */
    @Transaction
    suspend fun replaceAll(stocks: List<PortfolioStockEntity>) {
        deleteAll()
        insertAll(stocks)
    }
}
