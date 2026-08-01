package dev.comon.toss_watch.feature.tosskey.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.feature.tosskey.data.remote.TossKeyApi
import dev.comon.toss_watch.feature.tosskey.data.repository.GuestTossKeyRepository
import dev.comon.toss_watch.feature.tosskey.data.repository.TossKeyRepositoryImpl
import dev.comon.toss_watch.feature.tosskey.data.repository.TossKeyRepositoryRouter
import dev.comon.toss_watch.feature.tosskey.domain.repository.TossKeyRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object TossKeyDataModule {

    @Provides
    @Singleton
    fun provideTossKeyApi(retrofit: Retrofit): TossKeyApi =
        retrofit.create(TossKeyApi::class.java)

    // 게스트 모드에서는 실 API 호출 없이 항상 성공 처리하는 GuestTossKeyRepository로 라우팅한다.
    // @Binds는 컴파일 타임 고정이라 런타임 전환이 불가능해 @Provides 조립으로 대체한다.
    @Provides
    @Singleton
    fun provideTossKeyRepository(
        remote: TossKeyRepositoryImpl,
        guest: GuestTossKeyRepository,
        guestModeStore: GuestModeStore,
    ): TossKeyRepository = TossKeyRepositoryRouter(remote, guest, guestModeStore)
}
