package dev.comon.toss_watch.core.common.resources.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.common.resources.DefaultStringProvider
import dev.comon.toss_watch.core.common.resources.StringProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StringProviderModule {

    @Binds
    @Singleton
    abstract fun bindStringProvider(impl: DefaultStringProvider): StringProvider
}
