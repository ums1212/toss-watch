package dev.comon.watch_app.domain.repository

import dev.comon.toss_watch.core.model.watch.WatchAlarmRequest
import dev.comon.toss_watch.core.model.watch.WatchAlarmSnapshot
import kotlinx.coroutines.flow.Flow

data class WatchAlarmSyncState(
    val snapshot: WatchAlarmSnapshot? = null,
    val pending: WatchAlarmRequest? = null,
    val error: String? = null,
)

interface WatchAlarmRepository {
    fun observe(): Flow<WatchAlarmSyncState>
    suspend fun refresh()
    suspend fun submit(request: WatchAlarmRequest): Boolean
}
