package dev.comon.watch_app.service

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dev.comon.toss_watch.core.model.watch.WatchAlarmSync
import dev.comon.watch_app.data.repository.WatchAlarmRepositoryImpl
import dev.comon.watch_app.domain.usecase.RescheduleStockAlarmsUseCase
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class WatchAlarmSyncService : WearableListenerService() {
    @Inject lateinit var repository: WatchAlarmRepositoryImpl
    @Inject lateinit var rescheduleStockAlarms: RescheduleStockAlarmsUseCase

    // WearableListenerService delivers this callback on its background thread.
    override fun onDataChanged(events: DataEventBuffer) {
        var accepted = false
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith(WatchAlarmSync.SNAPSHOT_PREFIX)) continue
            val bytes = DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray(WatchAlarmSync.PAYLOAD) ?: continue
            runBlocking { repository.accept(path, bytes) }
            accepted = true
        }
        // 스냅샷 수신만으로 프로세스가 깨어난 경우에도 로컬 알람 예약을 바로 맞춘다.
        if (accepted) runBlocking { rescheduleStockAlarms() }
    }
}
