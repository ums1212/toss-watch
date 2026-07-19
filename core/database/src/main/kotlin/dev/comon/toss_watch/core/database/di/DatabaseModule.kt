package dev.comon.toss_watch.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.database.PortfolioStockCache
import dev.comon.toss_watch.core.database.PortfolioStockCacheImpl
import dev.comon.toss_watch.core.database.TossWatchDatabase
import dev.comon.toss_watch.core.database.dao.PortfolioStockDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindPortfolioStockCache(impl: PortfolioStockCacheImpl): PortfolioStockCache

    companion object {

        private const val DATABASE_NAME = "toss_watch.db"

        @Provides
        @Singleton
        fun provideTossWatchDatabase(
            @ApplicationContext context: Context,
        ): TossWatchDatabase =
            Room.databaseBuilder(context, TossWatchDatabase::class.java, DATABASE_NAME).build()

        @Provides
        @Singleton
        fun providePortfolioStockDao(database: TossWatchDatabase): PortfolioStockDao =
            database.portfolioStockDao()
    }
}
