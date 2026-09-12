package dev.comon.toss_watch.watchsync

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.WatchAlarmOutcome
import dev.comon.toss_watch.core.model.watch.WatchAlarmReceipt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** At-most-once submission; a timeout/crash cannot safely be retried without server idempotency. */
class PhoneAlarmRequestExecutor(
    private val read: suspend (String) -> WatchAlarmReceipt?,
    private val persist: suspend (WatchAlarmReceipt) -> Unit,
) {
    private val mutex = Mutex()

    suspend fun execute(id: String, action: suspend () -> NetworkResult<*>): WatchAlarmReceipt = mutex.withLock {
        read(id)?.let { return@withLock it }
        persist(WatchAlarmReceipt(id, WatchAlarmOutcome.UNKNOWN, "CHECK_PHONE"))
        val result = action()
        val outcome = when (result) {
            is NetworkResult.Success -> WatchAlarmOutcome.SAVED
            is NetworkResult.NetworkError -> WatchAlarmOutcome.UNKNOWN
            is NetworkResult.ApiError -> if (result.code >= 500) WatchAlarmOutcome.UNKNOWN else WatchAlarmOutcome.FAILED
        }
        val receipt = WatchAlarmReceipt(id, outcome, when (outcome) {
            WatchAlarmOutcome.SAVED -> null
            WatchAlarmOutcome.UNKNOWN -> "CHECK_PHONE"
            WatchAlarmOutcome.FAILED -> "SAVE_FAILED"
        })
        persist(receipt)
        receipt
    }
}
