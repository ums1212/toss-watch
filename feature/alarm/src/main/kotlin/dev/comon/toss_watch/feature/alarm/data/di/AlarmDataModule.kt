package dev.comon.toss_watch.feature.alarm.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.feature.alarm.data.remote.AlarmApi
import dev.comon.toss_watch.feature.alarm.data.repository.AlarmRepositoryImpl
import dev.comon.toss_watch.feature.alarm.data.repository.AlarmRepositoryRouter
import dev.comon.toss_watch.feature.alarm.data.repository.GuestAlarmRepository
import dev.comon.toss_watch.feature.alarm.domain.repository.AlarmRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object AlarmDataModule {

    @Provides
    @Singleton
    fun provideAlarmApi(retrofit: Retrofit): AlarmApi =
        retrofit.create(AlarmApi::class.java)

    // 게스트 모드에서는 실 API 호출 없이 인메모리 더미 캐시로 동작하는 GuestAlarmRepository로
    // 라우팅한다. @Binds는 컴파일 타임 고정이라 런타임 전환이 불가능해 @Provides 조립으로 대체한다.
    @Provides
    @Singleton
    fun provideAlarmRepository(
        remote: AlarmRepositoryImpl,
        guest: GuestAlarmRepository,
        guestModeStore: GuestModeStore,
    ): AlarmRepository = AlarmRepositoryRouter(remote, guest, guestModeStore)
}
