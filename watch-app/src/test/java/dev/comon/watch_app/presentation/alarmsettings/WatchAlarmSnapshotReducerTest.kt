package dev.comon.watch_app.presentation.alarmsettings

import dev.comon.toss_watch.core.model.watch.*
import dev.comon.watch_app.domain.repository.WatchAlarmSyncState
import dev.comon.watch_app.domain.repository.withSnapshot
import org.junit.Assert.*
import org.junit.Test

class WatchAlarmSnapshotReducerTest {
    private val pending = WatchAlarmRequest(id = "add", uuid = "watch", session = "session",
        operation = WatchAlarmOperation.ADD, stockCode = "A", days = listOf(0))
    private val snapshot = WatchAlarmSnapshot(uuid = "watch", session = "session", available = true, updatedAt = 10)
    private val state = WatchAlarmSyncState(snapshot, pending)

    @Test fun `unrelated list update must not confirm pending save`() {
        val next = state.withSnapshot(snapshot.copy(updatedAt = 11,
            receipt = WatchAlarmReceipt("other", WatchAlarmOutcome.SAVED)))
        assertEquals(pending, next.pending)
    }

    @Test fun `matching server receipt clears pending save`() {
        val next = state.withSnapshot(snapshot.copy(updatedAt = 11,
            receipt = WatchAlarmReceipt("add", WatchAlarmOutcome.SAVED)))
        assertNull(next.pending)
        assertNull(next.error)
    }

    @Test fun `unknown receipt is an explicit error and not a confirmed save`() {
        val next = state.withSnapshot(snapshot.copy(updatedAt = 11,
            receipt = WatchAlarmReceipt("add", WatchAlarmOutcome.UNKNOWN, "CHECK_PHONE")))
        assertNull(next.pending)
        assertEquals("CHECK_PHONE", next.error)
    }

    @Test fun `re-pair invalidates old account pending mutation`() {
        val next = state.withSnapshot(snapshot.copy(session = "other-account", updatedAt = 11))
        assertNull(next.pending)
        assertEquals("PAIR_PHONE", next.error)
    }

    @Test fun `out of order receipt cannot replace current snapshot or finish a pending command`() {
        val next = state.withSnapshot(snapshot.copy(updatedAt = 9,
            receipt = WatchAlarmReceipt("add", WatchAlarmOutcome.SAVED)))
        assertEquals(state, next)
    }

    @Test fun `invalid weekdays and times are rejected by transport contract`() {
        assertTrue(pending.isValid())
        assertFalse(pending.copy(days = emptyList()).isValid())
        assertFalse(pending.copy(days = listOf(0, 0)).isValid())
        assertFalse(pending.copy(days = listOf(7)).isValid())
        assertFalse(pending.copy(hour = 24).isValid())
        assertFalse(pending.copy(minute = 60).isValid())
    }

    @Test fun `failed refresh after phone restart retains last confirmed holdings`() {
        val stock = WatchStock("A", "Stock A")
        val cached = state.copy(snapshot = snapshot.copy(stocks = listOf(stock)))
        val next = cached.withSnapshot(snapshot.copy(available = false, updatedAt = 11, error = "REFRESH_FAILED"))
        assertTrue(next.snapshot!!.available)
        assertEquals(listOf(stock), next.snapshot!!.stocks)
    }

    @Test fun `revoked pairing clears cached holdings even in the same session`() {
        val cached = state.copy(snapshot = snapshot.copy(stocks = listOf(WatchStock("A", "Stock A"))))
        val next = cached.withSnapshot(snapshot.copy(available = false, updatedAt = 11, error = "PAIR_PHONE"))
        assertFalse(next.snapshot!!.available)
        assertTrue(next.snapshot!!.stocks.isEmpty())
        assertNull(next.pending)
    }
}
