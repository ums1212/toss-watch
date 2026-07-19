package dev.comon.watch_app.di

import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WatchAppModule {

    // SingletonComponent에 설치해야 @Singleton Repository(WatchPairingRepositoryImpl)에서도
    // 주입받을 수 있다(ViewModelComponent 바인딩은 그 하위 스코프에서만 접근 가능).
    @Provides
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
