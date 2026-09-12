package dev.comon.toss_watch.watchsync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.comon.toss_watch.core.model.watch.WatchAlarmRequest
import dev.comon.toss_watch.core.model.watch.WatchAlarmSync
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** Copy the event while its buffer is alive; WorkManager owns the subsequent network work. */
class PhoneAlarmSyncService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED ||
                event.dataItem.uri.path?.startsWith(WatchAlarmSync.REQUEST_PREFIX) != true) continue
            val bytes = DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray(WatchAlarmSync.PAYLOAD)
                ?: continue
            if (bytes.size > 8_000) continue
            val payload = bytes.decodeToString()
            val request = runCatching {
                Json.decodeFromString(WatchAlarmRequest.serializer(), payload)
            }.getOrNull() ?: continue
            if (request.isValid() && event.dataItem.uri.path == WatchAlarmSync.REQUEST_PREFIX + request.uuid) {
                PhoneAlarmSyncWorker.enqueue(this, payload)
            }
        }
    }
}

class PhoneAlarmSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val bridge = EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java).bridge()
        val payload = inputData.getString(PAYLOAD)
        if (payload == null) bridge.refresh() else {
            bridge.process(Json.decodeFromString(WatchAlarmRequest.serializer(), payload))
        }
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Retrying delivery is safe: the bridge persists a receipt before executing a mutation.
        if (runAttemptCount < 5) Result.retry() else Result.failure()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncEntryPoint { fun bridge(): PhoneAlarmSyncBridge }

    companion object {
        private const val PAYLOAD = "request"
        fun enqueue(context: Context, payload: String? = null) {
            val request = OneTimeWorkRequestBuilder<PhoneAlarmSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(PAYLOAD to payload))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "phone-alarm-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request,
            )
        }
    }
}
