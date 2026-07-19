package dev.comon.toss_watch.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.comon.toss_watch.core.database.dao.PortfolioStockDao
import dev.comon.toss_watch.core.database.entity.PortfolioStockEntity

@Database(
    entities = [PortfolioStockEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TossWatchDatabase : RoomDatabase() {
    abstract fun portfolioStockDao(): PortfolioStockDao
}
