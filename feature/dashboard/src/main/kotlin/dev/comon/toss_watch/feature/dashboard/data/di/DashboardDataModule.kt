package dev.comon.toss_watch.feature.dashboard.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.feature.dashboard.data.remote.DashboardApi
import dev.comon.toss_watch.feature.dashboard.data.repository.DashboardRepositoryImpl
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DashboardDataModule {

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    companion object {

        @Provides
        @Singleton
        fun provideDashboardApi(retrofit: Retrofit): DashboardApi =
            retrofit.create(DashboardApi::class.java)
    }
}
