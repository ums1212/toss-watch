package dev.comon.toss_watch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.comon.toss_watch.watchsync.PhoneAlarmSyncBridge
import javax.inject.Inject

@HiltAndroidApp
class TossWatchApplication : Application() {
    @Inject lateinit var alarmSyncBridge: PhoneAlarmSyncBridge

    override fun onCreate() {
        super.onCreate()
        alarmSyncBridge.start()
    }
}
