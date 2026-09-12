package dev.comon.watch_app.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.comon.toss_watch.core.model.watch.*
import dev.comon.watch_app.data.local.PairingPreferences
import dev.comon.watch_app.domain.repository.WatchAlarmRepository
import dev.comon.watch_app.domain.repository.WatchAlarmSyncState
import dev.comon.watch_app.domain.repository.withSnapshot
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

/** Persistent Data Layer inbox/outbox. A pending mutation is never reported as a saved alarm. */
@Singleton
class WatchAlarmRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val preferences: DataStore<Preferences>,
    private val pairing: PairingPreferences,
) : WatchAlarmRepository {
    private val client = Wearable.getDataClient(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    override fun observe(): Flow<WatchAlarmSyncState> = preferences.data.map { prefs ->
        WatchAlarmSyncState(
            snapshot = prefs[SNAPSHOT]?.let { json.decodeFromString(WatchAlarmSnapshot.serializer(), it) },
            pending = prefs[PENDING]?.let { json.decodeFromString(WatchAlarmRequest.serializer(), it) },
            error = prefs[ERROR],
        )
    }.distinctUntilChanged()

    fun start() {
        if (started) return
        started = true
        scope.launch {
            try {
                client.addListener { events ->
                    val copies = events.filter { it.type == DataEvent.TYPE_CHANGED }.mapNotNull { event ->
                        val path = event.dataItem.uri.path ?: return@mapNotNull null
                        if (!path.startsWith(WatchAlarmSync.SNAPSHOT_PREFIX)) return@mapNotNull null
                        DataMapItem.fromDataItem(event.dataItem).dataMap.getByteArray(WatchAlarmSync.PAYLOAD)?.let { path to it }
                    }
                    copies.forEach { (path, bytes) -> scope.launch { accept(path, bytes) } }
                }.await()
                val items = client.dataItems.await()
                try {
                    for (item in items) {
                        val path = item.uri.path ?: continue
                        if (!path.startsWith(WatchAlarmSync.SNAPSHOT_PREFIX)) continue
                        DataMapItem.fromDataItem(item).dataMap.getByteArray(WatchAlarmSync.PAYLOAD)?.let { accept(path, it) }
                    }
                } finally { items.release() }
                // Recover a request persisted immediately before a previous process was stopped.
                mutex.withLock { observe().first().pending?.let { transmit(it) } }
            } catch (e: CancellationException) { throw e } catch (_: Exception) {
                preferences.edit { it[ERROR] = "DELIVERY_FAILED" }
            }
        }
    }

    override suspend fun refresh() = mutex.withLock {
        val state = observe().first()
        val request = state.pending ?: WatchAlarmRequest(
            id = UUID.randomUUID().toString(), uuid = pairing.getOrCreateDeviceUuid(),
            session = state.snapshot?.session.orEmpty(), operation = WatchAlarmOperation.REFRESH,
        )
        savePending(request)
        transmit(request)
    }

    override suspend fun submit(request: WatchAlarmRequest): Boolean = mutex.withLock {
        val state = observe().first()
        if (state.pending != null && state.pending.operation != WatchAlarmOperation.REFRESH) return@withLock false
        val snapshot = state.snapshot
        if (snapshot?.available != true || request.session != snapshot.session ||
            request.uuid != pairing.getOrCreateDeviceUuid() || !request.isValid()) {
            preferences.edit { it[ERROR] = "PAIR_PHONE" }
            return@withLock false
        }
        savePending(request)
        transmit(request)
        true
    }

    suspend fun accept(path: String, bytes: ByteArray) = mutex.withLock {
        if (bytes.size > WatchAlarmSync.MAX_BYTES) return@withLock
        val snapshot = runCatching { json.decodeFromString(WatchAlarmSnapshot.serializer(), bytes.decodeToString()) }
            .getOrNull() ?: return@withLock
        val uuid = pairing.getOrCreateDeviceUuid()
        if (snapshot.version != WatchAlarmSync.VERSION || snapshot.uuid != uuid ||
            path != WatchAlarmSync.SNAPSHOT_PREFIX + uuid) return@withLock
        val current = observe().first()
        if (snapshot.updatedAt < (current.snapshot?.updatedAt ?: 0)) return@withLock
        val next = current.withSnapshot(snapshot)
        val completed = current.pending != null && next.pending == null
        preferences.edit {
            it[SNAPSHOT] = json.encodeToString(WatchAlarmSnapshot.serializer(), requireNotNull(next.snapshot))
            it.remove(ERROR)
            if (completed) it.remove(PENDING)
            next.error?.let { error -> it[ERROR] = error }
        }
        if (completed) {
            // Only remove our local DataItem. A phone snapshot is owned by the phone.
            try {
                val node = Wearable.getNodeClient(client.applicationContext).localNode.await()
                client.deleteDataItems(Uri.Builder().scheme("wear").authority(node.id)
                    .path(WatchAlarmSync.REQUEST_PREFIX + uuid).build()).await()
            } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Receipt makes redelivery safe. */ }
        }
    }

    private suspend fun savePending(request: WatchAlarmRequest) {
        preferences.edit {
            it[PENDING] = json.encodeToString(WatchAlarmRequest.serializer(), request)
            it.remove(ERROR)
        }
    }

    private suspend fun transmit(request: WatchAlarmRequest) {
        try {
            val data = PutDataMapRequest.create(WatchAlarmSync.REQUEST_PREFIX + request.uuid).apply {
                dataMap.putByteArray(WatchAlarmSync.PAYLOAD, json.encodeToString(WatchAlarmRequest.serializer(), request).toByteArray())
                // Re-send the same request ID on user retry; the phone will replay its receipt.
                dataMap.putLong("attempt", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            client.putDataItem(data).await()
        } catch (e: CancellationException) { throw e } catch (_: Exception) {
            preferences.edit { it[ERROR] = "DELIVERY_FAILED" }
        }
    }

    private companion object {
        val SNAPSHOT = stringPreferencesKey("alarm_sync_snapshot")
        val PENDING = stringPreferencesKey("alarm_sync_pending")
        val ERROR = stringPreferencesKey("alarm_sync_error")
    }
}
