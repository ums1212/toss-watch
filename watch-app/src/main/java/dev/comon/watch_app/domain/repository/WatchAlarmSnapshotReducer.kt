package dev.comon.watch_app.domain.repository

import dev.comon.toss_watch.core.model.watch.WatchAlarmOperation
import dev.comon.toss_watch.core.model.watch.WatchAlarmSnapshot

/** A list update alone cannot acknowledge a command; only its receipt or invalidated pairing can. */
internal fun WatchAlarmSyncState.withSnapshot(incoming: WatchAlarmSnapshot): WatchAlarmSyncState {
    if (incoming.updatedAt < (snapshot?.updatedAt ?: 0)) return this
    val request = pending
    val completed = request != null && (incoming.receipt?.requestId == request.id ||
        (request.operation != WatchAlarmOperation.REFRESH && request.session != incoming.session) ||
        (!incoming.available && incoming.error == "PAIR_PHONE"))
    val error = if (completed) {
        if (incoming.receipt?.requestId == request?.id) incoming.receipt?.error else "PAIR_PHONE"
    } else incoming.error
    val previous = snapshot
    val effective = if (previous != null && previous.session == incoming.session && !incoming.available &&
        incoming.error != "PAIR_PHONE" && incoming.error != "TOO_LARGE") {
        incoming.copy(available = previous.available, stocks = previous.stocks, alarms = previous.alarms)
    } else incoming
    return copy(snapshot = effective, pending = if (completed) null else request, error = error)
}
