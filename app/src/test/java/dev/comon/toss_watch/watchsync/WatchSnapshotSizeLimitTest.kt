package dev.comon.toss_watch.watchsync

import dev.comon.toss_watch.core.model.watch.WatchAlarm
import dev.comon.toss_watch.core.model.watch.WatchAlarmSnapshot
import dev.comon.toss_watch.core.model.watch.WatchStock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchSnapshotSizeLimitTest {
    private val alarms = (1L..3L).map {
        WatchAlarm(id = it, stockCode = "005930", stockName = "삼성전자", hour = 9, minute = 0, days = listOf(0), enabled = true)
    }
    private val stocks = (1..500).map { WatchStock(code = "A%05d".format(it), name = "종목 이름이 꽤 긴 테스트 종목 $it") }
    private val snapshot = WatchAlarmSnapshot(uuid = "watch", session = "s", available = true, stocks = stocks, alarms = alarms)

    @Test fun `snapshot within the limit is sent unchanged`() {
        val (fitted, _) = fitSnapshotToLimit(snapshot, maxBytes = Int.MAX_VALUE)
        assertEquals(snapshot, fitted)
        assertNull(fitted.error)
    }

    @Test fun `oversized snapshot drops stocks but keeps alarms so the watch keeps ringing`() {
        val (fitted, bytes) = fitSnapshotToLimit(snapshot, maxBytes = 2_000)
        assertTrue(bytes.size <= 2_000)
        assertEquals(alarms, fitted.alarms)
        assertTrue(fitted.stocks.isEmpty())
        assertFalse(fitted.available)
        assertEquals("TOO_LARGE", fitted.error)
    }

    @Test fun `alarms are dropped only when they alone exceed the limit`() {
        val (fitted, _) = fitSnapshotToLimit(snapshot, maxBytes = 100)
        assertTrue(fitted.alarms.isEmpty())
        assertEquals("TOO_LARGE", fitted.error)
    }
}
