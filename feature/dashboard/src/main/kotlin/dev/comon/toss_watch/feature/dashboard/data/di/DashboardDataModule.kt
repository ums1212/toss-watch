package dev.comon.toss_watch.feature.dashboard.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.feature.dashboard.data.remote.DashboardApi
import dev.comon.toss_watch.feature.dashboard.data.repository.DashboardRepositoryImpl
import dev.comon.toss_watch.feature.dashboard.data.repository.DashboardRepositoryRouter
import dev.comon.toss_watch.feature.dashboard.data.repository.GuestDashboardRepository
import dev.comon.toss_watch.feature.dashboard.domain.repository.DashboardRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object DashboardDataModule {

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi =
        retrofit.create(DashboardApi::class.java)

    // 게스트 모드에서는 실 API/Room 캐시를 건드리지 않는 GuestDashboardRepository로 라우팅한다.
    // @Binds는 컴파일 타임 고정이라 런타임 전환이 불가능해 @Provides 조립으로 대체한다.
    @Provides
    @Singleton
    fun provideDashboardRepository(
        remote: DashboardRepositoryImpl,
        guest: GuestDashboardRepository,
        guestModeStore: GuestModeStore,
    ): DashboardRepository = DashboardRepositoryRouter(remote, guest, guestModeStore)
}
