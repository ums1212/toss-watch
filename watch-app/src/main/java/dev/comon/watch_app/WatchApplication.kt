package dev.comon.watch_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.comon.watch_app.data.repository.WatchAlarmRepositoryImpl
import javax.inject.Inject
import android.content.Context
import dev.comon.watch_app.diagnostics.StartupTiming
import dev.comon.watch_app.domain.usecase.ObserveWatchAlarmsUseCase
import dev.comon.watch_app.domain.usecase.RescheduleStockAlarmsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class WatchApplication : Application() {
    @Inject lateinit var alarmRepository: WatchAlarmRepositoryImpl
    @Inject lateinit var observeWatchAlarms: ObserveWatchAlarmsUseCase
    @Inject lateinit var rescheduleStockAlarms: RescheduleStockAlarmsUseCase
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        StartupTiming.mark("application.attach")
    }
    override fun onCreate() {
        val startedAt = StartupTiming.now()
        super.onCreate()
        alarmRepository.start()
        // 폰에서 동기화된 알람 목록이 바뀔 때마다(앱 시작 시 1회 포함) 로컬 알람 예약을 맞춘다.
        appScope.launch {
            observeWatchAlarms().map { it.snapshot?.alarms }.distinctUntilChanged().collect {
                rescheduleStockAlarms()
            }
        }
        StartupTiming.mark("application.onCreate", startedAt)
    }
}
