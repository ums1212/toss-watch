package dev.comon.watch_app.presentation.alarmsettings

import androidx.lifecycle.SavedStateHandle
import dev.comon.toss_watch.core.model.watch.*
import dev.comon.watch_app.domain.repository.WatchAlarmRepository
import dev.comon.watch_app.domain.repository.WatchAlarmSyncState
import dev.comon.watch_app.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchAlarmViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = WatchAlarmViewModel(
        ObserveWatchAlarmsUseCase(repository), RefreshWatchAlarmsUseCase(repository), SaveWatchAlarmUseCase(repository), handle,
    )

    @Test fun `double tap submits once and does not optimistically create an alarm`() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.handleIntent(WatchAlarmIntent.Add("005930", "삼성전자"))
        vm.handleIntent(WatchAlarmIntent.Add("005930", "삼성전자"))
        runCurrent()
        assertEquals(1, repository.requests.size)
        assertTrue(vm.uiState.value.sync.snapshot!!.alarms.isEmpty())
        assertTrue(vm.uiState.value.busy)
    }

    @Test fun `no weekdays prevents submission`() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        (0..5).forEach { vm.handleIntent(WatchAlarmIntent.ToggleDay(it)) }
        vm.handleIntent(WatchAlarmIntent.Add("005930", "삼성전자"))
        runCurrent()
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun `draft retains time and days when restored`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.handleIntent(WatchAlarmIntent.SetTime(23, 59))
        vm.handleIntent(WatchAlarmIntent.ToggleDay(6))
        val restored = viewModel(handle)
        assertEquals(23, restored.uiState.value.hour)
        assertEquals(59, restored.uiState.value.minute)
        assertEquals((0..6).toSet(), restored.uiState.value.days)
    }

    @Test fun `cached data can be edited while an offline refresh is pending`() = runTest(dispatcher) {
        repository.state.value = repository.state.value.copy(pending = WatchAlarmRequest(
            id = "refresh", uuid = "watch", session = "session", operation = WatchAlarmOperation.REFRESH))
        val vm = viewModel()
        runCurrent()
        assertFalse(vm.uiState.value.busy)
        vm.handleIntent(WatchAlarmIntent.Add("005930", "삼성전자"))
        runCurrent()
        assertEquals(1, repository.requests.size)
    }

    @Test fun `unpaired snapshot prevents changes`() = runTest(dispatcher) {
        repository.state.value = WatchAlarmSyncState()
        val vm = viewModel()
        runCurrent()
        vm.handleIntent(WatchAlarmIntent.Add("005930", "삼성전자"))
        runCurrent()
        assertTrue(repository.requests.isEmpty())
    }

    private class FakeRepository : WatchAlarmRepository {
        val state = MutableStateFlow(WatchAlarmSyncState(snapshot = WatchAlarmSnapshot(
            uuid = "watch", session = "session", available = true,
            stocks = listOf(WatchStock("005930", "삼성전자")),
        )))
        val requests = mutableListOf<WatchAlarmRequest>()
        override fun observe() = state
        override suspend fun refresh() = Unit
        override suspend fun submit(request: WatchAlarmRequest): Boolean {
            requests += request
            state.value = state.value.copy(pending = request)
            return true
        }
    }
}
