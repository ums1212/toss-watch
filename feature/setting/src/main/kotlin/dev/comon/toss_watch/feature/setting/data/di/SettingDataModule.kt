package dev.comon.toss_watch.feature.setting.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.feature.setting.data.remote.SettingApi
import dev.comon.toss_watch.feature.setting.data.repository.GuestSettingRepository
import dev.comon.toss_watch.feature.setting.data.repository.SettingRepositoryImpl
import dev.comon.toss_watch.feature.setting.data.repository.SettingRepositoryRouter
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object SettingDataModule {

    @Provides
    @Singleton
    fun provideSettingApi(retrofit: Retrofit): SettingApi =
        retrofit.create(SettingApi::class.java)

    // 게스트 모드에서는 실 API 호출 없이 인메모리로 동작하는 GuestSettingRepository로 라우팅한다.
    // @Binds는 컴파일 타임 고정이라 런타임 전환이 불가능해 @Provides 조립으로 대체한다.
    @Provides
    @Singleton
    fun provideSettingRepository(
        remote: SettingRepositoryImpl,
        guest: GuestSettingRepository,
        guestModeStore: GuestModeStore,
    ): SettingRepository = SettingRepositoryRouter(remote, guest, guestModeStore)
}
