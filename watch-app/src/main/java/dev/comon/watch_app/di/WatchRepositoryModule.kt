package dev.comon.watch_app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.watch_app.data.repository.WatchPairingRepositoryImpl
import dev.comon.watch_app.domain.repository.WatchPairingRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WatchRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWatchPairingRepository(impl: WatchPairingRepositoryImpl): WatchPairingRepository
}
