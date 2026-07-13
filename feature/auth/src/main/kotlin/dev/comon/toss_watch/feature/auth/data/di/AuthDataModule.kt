package dev.comon.toss_watch.feature.auth.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.feature.auth.data.remote.AuthApi
import dev.comon.toss_watch.feature.auth.data.repository.AuthRepositoryImpl
import dev.comon.toss_watch.feature.auth.domain.repository.AuthRepository
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthDataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {

        @Provides
        @Singleton
        fun provideAuthApi(retrofit: Retrofit): AuthApi =
            retrofit.create(AuthApi::class.java)
    }
}
