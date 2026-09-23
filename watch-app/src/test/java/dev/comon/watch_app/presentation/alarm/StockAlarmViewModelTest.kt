package dev.comon.watch_app.presentation.alarm

import androidx.lifecycle.SavedStateHandle
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.watch_app.domain.model.StockQuote
import dev.comon.watch_app.domain.repository.WatchStockQuoteRepository
import dev.comon.watch_app.domain.usecase.FetchStockQuoteUseCase
import dev.comon.watch_app.service.StockAlarmNotifications
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StockAlarmViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeQuoteRepository()
    private val quote = StockQuote("005930", "삼성전자", "72500", "+2.30%")

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel() = StockAlarmViewModel(
        FetchStockQuoteUseCase(repository),
        SavedStateHandle(mapOf(
            StockAlarmNotifications.EXTRA_STOCK_CODE to "005930",
            StockAlarmNotifications.EXTRA_STOCK_NAME to "삼성전자",
        )),
    )

    @Test fun `quote is prefetched while the alarm is still ringing`() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        assertEquals(listOf("005930"), repository.requests)
        assertFalse(vm.uiState.value.opened)
        repository.response.complete(NetworkResult.Success(quote))
        runCurrent()
        assertEquals(StockQuoteState.Loaded(quote), vm.uiState.value.quote)
    }

    @Test fun `opening before the response shows loading then the quote`() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.handleIntent(StockAlarmIntent.Open)
        assertTrue(vm.uiState.value.opened)
        assertEquals(StockQuoteState.Loading, vm.uiState.value.quote)
        repository.response.complete(NetworkResult.Success(quote))
        runCurrent()
        assertEquals(StockQuoteState.Loaded(quote), vm.uiState.value.quote)
        assertEquals(1, repository.requests.size)
    }

    @Test fun `error can be retried`() = runTest(dispatcher) {
        repository.response.complete(NetworkResult.NetworkError(IOException()))
        val vm = viewModel()
        runCurrent()
        assertEquals(StockQuoteState.Error, vm.uiState.value.quote)
        repository.response = CompletableDeferred(NetworkResult.Success(quote))
        vm.handleIntent(StockAlarmIntent.Retry)
        runCurrent()
        assertEquals(StockQuoteState.Loaded(quote), vm.uiState.value.quote)
        assertEquals(2, repository.requests.size)
    }

    @Test fun `a new alarm resets the screen and fetches the new stock`() = runTest(dispatcher) {
        repository.response.complete(NetworkResult.Success(quote))
        val vm = viewModel()
        runCurrent()
        vm.handleIntent(StockAlarmIntent.Open)
        vm.handleIntent(StockAlarmIntent.NewAlarm("000660", "SK하이닉스"))
        runCurrent()
        val state = vm.uiState.value
        assertFalse(state.opened)
        assertEquals("SK하이닉스", state.stockName)
        assertEquals(1, state.alarmVersion)
        assertEquals(listOf("005930", "000660"), repository.requests)
    }

    private class FakeQuoteRepository : WatchStockQuoteRepository {
        val requests = mutableListOf<String>()
        var response = CompletableDeferred<NetworkResult<StockQuote>>()
        override suspend fun fetch(stockCode: String): NetworkResult<StockQuote> {
            requests += stockCode
            return response.await()
        }
    }
}
