package dev.comon.toss_watch.feature.alarm.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.feature.alarm.data.remote.AlarmApi
import dev.comon.toss_watch.feature.alarm.data.repository.AlarmRepositoryImpl
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AlarmDataModule {

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(impl: AlarmRepositoryImpl): AlarmRepository

    companion object {

        @Provides
        @Singleton
        fun provideAlarmApi(retrofit: Retrofit): AlarmApi =
            retrofit.create(AlarmApi::class.java)
    }
}
