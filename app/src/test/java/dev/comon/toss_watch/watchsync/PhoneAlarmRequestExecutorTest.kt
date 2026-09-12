package dev.comon.toss_watch.watchsync

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.WatchAlarmOutcome
import dev.comon.toss_watch.core.model.watch.WatchAlarmReceipt
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PhoneAlarmRequestExecutorTest {
    private val receipts = mutableMapOf<String, WatchAlarmReceipt>()
    private fun executor() = PhoneAlarmRequestExecutor({ receipts[it] }, { receipts[it.requestId] = it })

    @Test fun `redelivery after process restart replays success without another POST`() = runTest {
        var posts = 0
        val first = executor().execute("add-1") { posts++; NetworkResult.Success(Unit) }
        val second = executor().execute("add-1") { posts++; NetworkResult.Success(Unit) }
        assertEquals(1, posts)
        assertEquals(WatchAlarmOutcome.SAVED, first.outcome)
        assertEquals(first, second)
    }

    @Test fun `process cancellation after submission never resubmits an uncertain POST`() = runTest {
        try {
            executor().execute("add-2") { throw CancellationException("process stopped") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        var posts = 0
        val result = executor().execute("add-2") { posts++; NetworkResult.Success(Unit) }
        assertEquals(0, posts)
        assertEquals(WatchAlarmOutcome.UNKNOWN, result.outcome)
    }

    @Test fun `timeout is uncertain rather than falsely reporting a save failure`() = runTest {
        val result = executor().execute("add-3") { NetworkResult.NetworkError(IOException("timeout")) }
        assertEquals(WatchAlarmOutcome.UNKNOWN, result.outcome)
    }

    @Test fun `rejected request is retained and not retried on delivery`() = runTest {
        val result = executor().execute("add-4") { NetworkResult.ApiError(400, "invalid schedule") }
        assertEquals(WatchAlarmOutcome.FAILED, result.outcome)
        assertEquals(result, executor().execute("add-4") { fail("Must not resubmit"); NetworkResult.Success(Unit) })
    }

    @Test fun `concurrent delivery of one request executes once`() = runTest {
        var posts = 0
        val executor = executor()
        val first = async { executor.execute("add-5") { posts++; delay(10); NetworkResult.Success(Unit) } }
        val second = async { executor.execute("add-5") { posts++; NetworkResult.Success(Unit) } }
        assertEquals(first.await(), second.await())
        assertEquals(1, posts)
    }
}
