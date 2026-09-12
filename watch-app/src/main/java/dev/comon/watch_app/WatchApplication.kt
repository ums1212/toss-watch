package dev.comon.watch_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.comon.watch_app.data.repository.WatchAlarmRepositoryImpl
import javax.inject.Inject

@HiltAndroidApp
class WatchApplication : Application() {
    @Inject lateinit var alarmRepository: WatchAlarmRepositoryImpl
    override fun onCreate() {
        super.onCreate()
        alarmRepository.start()
    }
}
