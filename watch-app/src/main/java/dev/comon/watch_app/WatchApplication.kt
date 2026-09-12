package dev.comon.watch_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.comon.watch_app.data.repository.WatchAlarmRepositoryImpl
import javax.inject.Inject
import android.content.Context
import dev.comon.watch_app.diagnostics.StartupTiming

@HiltAndroidApp
class WatchApplication : Application() {
    @Inject lateinit var alarmRepository: WatchAlarmRepositoryImpl
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        StartupTiming.mark("application.attach")
    }
    override fun onCreate() {
        val startedAt = StartupTiming.now()
        super.onCreate()
        alarmRepository.start()
        StartupTiming.mark("application.onCreate", startedAt)
    }
}
