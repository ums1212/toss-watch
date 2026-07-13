package dev.comon.toss_watch.core.common.coroutine.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.common.coroutine.DefaultDispatcherProvider
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoroutineModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
