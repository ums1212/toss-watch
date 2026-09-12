package dev.comon.watch_app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.watch_app.data.repository.WatchAlarmRepositoryImpl
import dev.comon.watch_app.domain.repository.WatchAlarmRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchAlarmModule {
    @Binds abstract fun bindAlarmRepository(impl: WatchAlarmRepositoryImpl): WatchAlarmRepository
}
