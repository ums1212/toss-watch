package dev.comon.toss_watch.watchsync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.comon.toss_watch.core.datastore.GuestModeStore
import dev.comon.toss_watch.core.datastore.TokenStore
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.*
import dev.comon.toss_watch.feature.alarm.domain.usecase.*
import dev.comon.toss_watch.feature.setting.domain.usecase.SyncPairedWatchUseCase
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

/** Application-level orchestration of independent features through their UseCases. */
@Singleton
class PhoneAlarmSyncBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokens: TokenStore,
    private val guestMode: GuestModeStore,
    private val store: PhoneAlarmSyncStore,
    private val observeStocks: ObservePortfolioStocksUseCase,
    private val observeAlarms: ObserveAlarmProfilesUseCase,
    private val fetchAlarms: FetchAlarmProfilesUseCase,
    private val addAlarm: AddAlarmProfileUseCase,
    private val toggleAlarm: ToggleAlarmProfileUseCase,
    private val deleteAlarm: DeleteAlarmProfileUseCase,
    private val syncPairing: SyncPairedWatchUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val executor = PhoneAlarmRequestExecutor(store::receipt, store::saveReceipt)
    private var started = false
    private var readySession: String? = null
    private var observedSession: String? = null

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(tokens.observeHasSession(), tokens.observePairedWatch(), guestMode.observeGuestMode()) { _, _, _ -> Unit }
                .collect {
                    safely {
                        mutex.withLock {
                            val identity = identity()
                            if (identity != null && observedSession != identity.session) {
                                observedSession = identity.session
                                readySession = null
                                PhoneAlarmSyncWorker.enqueue(context)
                            }
                        }
                    }
                }
        }
        scope.launch {
            combine(observeStocks(), observeAlarms()) { _, _ -> Unit }.collect {
                safely { mutex.withLock { identity()?.let { publish(it) } } }
            }
        }
        scope.launch {
            safely {
                // Recover DataItems received while the old phone app/process was not listening.
                val items = Wearable.getDataClient(context).dataItems.await()
                try {
                    for (item in items) {
                        val path = item.uri.path ?: continue
                        if (!path.startsWith(WatchAlarmSync.REQUEST_PREFIX)) continue
                        val bytes = DataMapItem.fromDataItem(item).dataMap.getByteArray(WatchAlarmSync.PAYLOAD) ?: continue
                        if (bytes.size > 8_000) continue
                        val payload = bytes.decodeToString()
                        val request = runCatching { Json.decodeFromString(WatchAlarmRequest.serializer(), payload) }.getOrNull()
                        if (request?.isValid() == true && path == WatchAlarmSync.REQUEST_PREFIX + request.uuid) {
                            PhoneAlarmSyncWorker.enqueue(context, payload)
                        }
                    }
                } finally { items.release() }
            }
        }
    }

    suspend fun refresh() = mutex.withLock {
        verifyPairing()
        val identity = identity() ?: return@withLock
        val result = fetchAlarms()
        if (result is NetworkResult.Success) readySession = identity.session
        publish(identity, if (result is NetworkResult.Success) null else "REFRESH_FAILED")
    }

    suspend fun process(request: WatchAlarmRequest) = mutex.withLock {
        if (!request.isValid()) return@withLock
        verifyPairing()
        val identity = identity()
        if (identity == null || identity.uuid != request.uuid) {
            send(WatchAlarmSnapshot(uuid = request.uuid, error = "PAIR_PHONE"))
            return@withLock
        }
        if (request.operation != WatchAlarmOperation.REFRESH && request.session != identity.session) {
            publish(identity, "PAIR_PHONE", WatchAlarmReceipt(request.id, WatchAlarmOutcome.FAILED, "PAIR_PHONE"))
            return@withLock
        }
        store.receipt(request.id)?.let {
            // A previous process may have died after POST succeeded but before saving its response.
            val result = fetchAlarms()
            if (result is NetworkResult.Success) readySession = identity.session
            publish(identity, receipt = it)
            return@withLock
        }

        if (request.operation == WatchAlarmOperation.REFRESH) {
            val result = fetchAlarms()
            if (result is NetworkResult.Success) readySession = identity.session
            val receipt = WatchAlarmReceipt(request.id,
                if (result is NetworkResult.Success) WatchAlarmOutcome.SAVED else WatchAlarmOutcome.FAILED,
                if (result is NetworkResult.Success) null else "REFRESH_FAILED")
            store.saveReceipt(receipt)
            publish(identity, receipt = receipt)
            return@withLock
        }

        // Validate against the phone's current data; never apply arbitrary/stale stock names or IDs.
        if (fetchAlarms() !is NetworkResult.Success) {
            val receipt = WatchAlarmReceipt(request.id, WatchAlarmOutcome.FAILED, "REFRESH_FAILED")
            store.saveReceipt(receipt)
            publish(identity, receipt = receipt)
            return@withLock
        }
        readySession = identity.session
        val stocks = observeStocks().first()
        val alarms = observeAlarms().first()
        val stock = stocks.firstOrNull { it.stockCode == request.stockCode }
        val validTarget = when (request.operation) {
            WatchAlarmOperation.ADD -> stock != null
            else -> alarms.any { it.id == request.alarmId }
        }
        // Recheck after suspension: a logout/re-pair may have happened during the fetch.
        if (identity() != identity) return@withLock
        if (!validTarget) {
            val receipt = WatchAlarmReceipt(request.id, WatchAlarmOutcome.FAILED, "STALE_DATA")
            store.saveReceipt(receipt)
            publish(identity, receipt = receipt)
            return@withLock
        }
        val receipt = executor.execute(request.id) { when (request.operation) {
            WatchAlarmOperation.ADD -> addAlarm(requireNotNull(stock).stockCode, stock.stockName,
                request.hour, request.minute, request.days.sorted())
            WatchAlarmOperation.TOGGLE -> toggleAlarm(request.alarmId, request.enabled)
            WatchAlarmOperation.DELETE -> deleteAlarm(request.alarmId)
            WatchAlarmOperation.REFRESH -> error("Handled above")
        } }
        if (identity() != identity) return@withLock
        publish(identity, receipt = receipt)
    }

    private suspend fun identity(): Identity? {
        val paired = tokens.observePairedWatch().first()
        val refreshToken = tokens.getRefreshToken()
        if (paired == null || refreshToken == null || guestMode.isGuestMode()) {
            store.previousUuid()?.let { send(WatchAlarmSnapshot(uuid = it, error = "PAIR_PHONE")) }
            store.clear()
            readySession = null
            observedSession = null
            return null
        }
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest("$refreshToken|${paired.uuid}|${paired.linkedAt}".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val previous = store.previousUuid()
        val previousSession = store.previousSession()
        if (previous != null && previous != paired.uuid) send(WatchAlarmSnapshot(uuid = previous, error = "PAIR_PHONE"))
        val session = store.session(fingerprint, paired.uuid)
        if (previousSession != null && previousSession != session) {
            send(WatchAlarmSnapshot(uuid = paired.uuid, session = session, error = "PAIR_PHONE"))
        }
        return Identity(paired.uuid, session)
    }

    private suspend fun verifyPairing() {
        if (tokens.getRefreshToken() != null && !guestMode.isGuestMode() && syncPairing() !is NetworkResult.Success) {
            // No mutation has been submitted yet; WorkManager can safely retry this lookup.
            throw IOException("Could not verify the registered watch")
        }
    }

    private suspend fun publish(identity: Identity, error: String? = null, receipt: WatchAlarmReceipt? = null) {
        if (identity() != identity) return
        val ready = readySession == identity.session
        // An offline phone restart must not erase the watch's last confirmed list.
        if (!ready && error == null && receipt == null) return
        send(WatchAlarmSnapshot(
            uuid = identity.uuid, session = identity.session, available = ready,
            stocks = if (ready) observeStocks().first().map { WatchStock(it.stockCode, it.stockName) } else emptyList(),
            alarms = if (ready) observeAlarms().first().map {
                WatchAlarm(it.id, it.stockCode, it.stockName, it.hour, it.minute, it.daysOfWeek, it.isEnabled, it.disabledReason)
            } else emptyList(),
            receipt = receipt ?: store.lastReceipt(), error = error,
        ))
    }

    private suspend fun send(snapshot: WatchAlarmSnapshot) {
        val (_, bytes) = fitSnapshotToLimit(snapshot.copy(updatedAt = store.nextRevision()))
        val item = PutDataMapRequest.create(WatchAlarmSync.SNAPSHOT_PREFIX + snapshot.uuid).apply {
            dataMap.putByteArray(WatchAlarmSync.PAYLOAD, bytes)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(item).await()
    }

    private suspend fun safely(block: suspend () -> Unit) {
        try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) {
            PhoneAlarmSyncWorker.enqueue(context)
        }
    }

    private data class Identity(val uuid: String, val session: String)
}
