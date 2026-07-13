package dev.comon.toss_watch.feature.setting.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.feature.setting.data.remote.SettingApi
import dev.comon.toss_watch.feature.setting.data.repository.SettingRepositoryImpl
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingDataModule {

    @Binds
    @Singleton
    abstract fun bindSettingRepository(impl: SettingRepositoryImpl): SettingRepository

    companion object {

        @Provides
        @Singleton
        fun provideSettingApi(retrofit: Retrofit): SettingApi =
            retrofit.create(SettingApi::class.java)
    }
}
