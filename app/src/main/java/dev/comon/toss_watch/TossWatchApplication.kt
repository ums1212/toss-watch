package dev.comon.toss_watch

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import dev.comon.toss_watch.watchsync.PhoneAlarmImageSync
import dev.comon.toss_watch.watchsync.PhoneAlarmSyncBridge
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class TossWatchApplication : Application() {
    @Inject lateinit var alarmSyncBridge: PhoneAlarmSyncBridge
    @Inject lateinit var alarmImageSync: PhoneAlarmImageSync

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        alarmSyncBridge.start()
        alarmImageSync.start()
        // Mobile Ads SDK 초기화는 디스크 I/O를 동반하므로 앱 시작(메인 스레드)을 막지 않게
        // IO 디스패처에서 수행한다.
        applicationScope.launch { MobileAds.initialize(this@TossWatchApplication) }
    }
}
