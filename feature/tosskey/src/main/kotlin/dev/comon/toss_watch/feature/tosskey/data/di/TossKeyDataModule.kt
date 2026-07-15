package dev.comon.toss_watch.feature.tosskey.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.feature.tosskey.data.remote.TossKeyApi
import dev.comon.toss_watch.feature.tosskey.data.repository.TossKeyRepositoryImpl
import dev.comon.toss_watch.feature.tosskey.domain.repository.TossKeyRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TossKeyDataModule {

    @Binds
    @Singleton
    abstract fun bindTossKeyRepository(impl: TossKeyRepositoryImpl): TossKeyRepository

    companion object {

        @Provides
        @Singleton
        fun provideTossKeyApi(retrofit: Retrofit): TossKeyApi =
            retrofit.create(TossKeyApi::class.java)
    }
}
