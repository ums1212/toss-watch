package dev.comon.toss_watch.feature.setting.presentation

import dev.comon.toss_watch.feature.setting.domain.usecase.AddAlarmProfileUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.DeleteAlarmProfileUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.usecase.LogoutUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObservePairedWatchUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObservePortfolioStocksUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.SyncPairedWatchUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ToggleAlarmProfileUseCase
import dev.comon.toss_watch.feature.setting.util.FakeSettingRepository
import dev.comon.toss_watch.feature.setting.util.MainDispatcherRule
import dev.comon.toss_watch.feature.setting.util.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeSettingRepository()

    private fun createViewModel(): SettingViewModel =
        SettingViewModel(
            fetchAlarmProfilesUseCase = FetchAlarmProfilesUseCase(fakeRepository),
            addAlarmProfileUseCase = AddAlarmProfileUseCase(fakeRepository),
            toggleAlarmProfileUseCase = ToggleAlarmProfileUseCase(fakeRepository),
            deleteAlarmProfileUseCase = DeleteAlarmProfileUseCase(fakeRepository),
            observePortfolioStocksUseCase = ObservePortfolioStocksUseCase(fakeRepository),
            observePairedWatchUseCase = ObservePairedWatchUseCase(fakeRepository),
            syncPairedWatchUseCase = SyncPairedWatchUseCase(fakeRepository),
            logoutUseCase = LogoutUseCase(fakeRepository),
            dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )

    private fun TestScope.collectSideEffects(
        viewModel: SettingViewModel,
    ): List<SettingUiSideEffect> {
        val effects = mutableListOf<SettingUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sideEffect.toList(effects)
        }
        return effects
    }

    @Test
    fun `초기 로드 성공 시 등록된 알람 목록이 상태에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(FakeSettingRepository.DEFAULT_ALARMS, state.configuredAlarms)
        }

    @Test
    fun `init 시 syncPairedWatch가 호출되어 서버에 복원된 워치 정보가 상태에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.syncedWatch = PairedWatchInfo(modelName = "Galaxy Watch 6", uuid = "uuid-abc")

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(1, fakeRepository.syncInvocationCount)
            assertEquals(
                PairedWatchInfo(modelName = "Galaxy Watch 6", uuid = "uuid-abc"),
                viewModel.uiState.value.pairedWatch,
            )
        }

    @Test
    fun `OnPairWatchClicked는 NavigateToWatchPair 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(SettingUiIntent.OnPairWatchClicked)
            runCurrent()

            assertEquals(
                listOf<SettingUiSideEffect>(SettingUiSideEffect.NavigateToWatchPair),
                effects,
            )
        }

    @Test
    fun `OnAddAlarm 성공 시 새 프로필이 목록에 추가되고 토스트가 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(SettingUiIntent.OnAddAlarm("000660", 10, 15, listOf(0, 1, 2, 3, 4, 5)))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("000660", fakeRepository.lastAddedStockCode)
            assertEquals(10, fakeRepository.lastAddedHour)
            assertEquals(15, fakeRepository.lastAddedMinute)
            assertEquals(listOf(0, 1, 2, 3, 4, 5), fakeRepository.lastAddedDaysOfWeek)
            assertEquals(
                FakeSettingRepository.DEFAULT_ALARMS + FakeSettingRepository.ADDED_ALARM,
                state.configuredAlarms,
            )
            assertEquals(
                listOf<SettingUiSideEffect>(
                    SettingUiSideEffect.ShowToast(SettingViewModel.TOAST_ALARM_ADDED),
                ),
                effects,
            )
        }

    @Test
    fun `OnDeleteAlarm 성공 시 해당 프로필이 목록에서 제거되고 토스트가 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(SettingUiIntent.OnDeleteAlarm(alarmId = 2L))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2L, fakeRepository.lastDeletedId)
            assertEquals(1, fakeRepository.deleteInvocationCount)
            assertEquals(
                FakeSettingRepository.DEFAULT_ALARMS.filterNot { it.id == 2L },
                state.configuredAlarms,
            )
            assertEquals(
                listOf<SettingUiSideEffect>(
                    SettingUiSideEffect.ShowToast(SettingViewModel.TOAST_ALARM_DELETED),
                ),
                effects,
            )
        }

    @Test
    fun `OnDeleteAlarm 실패 시 목록은 그대로 유지되고 에러가 표시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.deleteResult = NetworkResult.ApiError(code = 500, message = "삭제 서버 오류")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnDeleteAlarm(alarmId = 2L))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(FakeSettingRepository.DEFAULT_ALARMS, state.configuredAlarms)
            assertEquals("삭제 서버 오류", state.errorMessage)
        }

    @Test
    fun `OnToggleAlarm 성공 시 해당 프로필만 갱신된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnToggleAlarm(alarmId = 2L, enabled = true))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2L, fakeRepository.lastToggledId)
            assertEquals(true, fakeRepository.lastToggledEnabled)
            assertTrue(state.configuredAlarms.first { it.id == 2L }.isEnabled)
            // 나머지 프로필은 그대로 유지된다.
            assertTrue(state.configuredAlarms.first { it.id == 1L }.isEnabled)
        }

    @Test
    fun `OnToggleAlarm은 API 응답을 기다리지 않고 즉시 로컬 상태를 낙관적으로 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnToggleAlarm(alarmId = 2L, enabled = true))
            runCurrent() // 500ms 디바운스가 지나기 전 — 아직 API는 호출되지 않은 시점.

            assertTrue(viewModel.uiState.value.configuredAlarms.first { it.id == 2L }.isEnabled)
            assertEquals(0, fakeRepository.toggleInvocationCount)
        }

    @Test
    fun `동일 알람을 연타하면 마지막 값만 디바운스 후 한 번만 API에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnToggleAlarm(alarmId = 2L, enabled = true))
            viewModel.handleIntent(SettingUiIntent.OnToggleAlarm(alarmId = 2L, enabled = false))
            viewModel.handleIntent(SettingUiIntent.OnToggleAlarm(alarmId = 2L, enabled = true))
            advanceUntilIdle()

            assertEquals(1, fakeRepository.toggleInvocationCount)
            assertEquals(true, fakeRepository.lastToggledEnabled)
            assertTrue(viewModel.uiState.value.configuredAlarms.first { it.id == 2L }.isEnabled)
        }

    @Test
    fun `OnToggleAlarm 실패 시 낙관적으로 반영했던 값을 이전 상태로 되돌린다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.toggleResult = NetworkResult.ApiError(
                code = 500,
                message = "서버 오류",
            )
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnToggleAlarm(alarmId = 1L, enabled = false))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.configuredAlarms.first { it.id == 1L }.isEnabled)
            assertEquals("서버 오류", state.errorMessage)
        }

    @Test
    fun `OnBackClicked는 NavigateBack 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(SettingUiIntent.OnBackClicked)
            runCurrent()

            assertEquals(
                listOf<SettingUiSideEffect>(SettingUiSideEffect.NavigateBack),
                effects,
            )
        }

    @Test
    fun `OnTossKeyClicked는 NavigateToTossKey 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(SettingUiIntent.OnTossKeyClicked)
            runCurrent()

            assertEquals(
                listOf<SettingUiSideEffect>(SettingUiSideEffect.NavigateToTossKey),
                effects,
            )
        }

    @Test
    fun `OnLogoutClicked는 저장소의 logout을 호출한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnLogoutClicked)
            advanceUntilIdle()

            assertEquals(1, fakeRepository.logoutInvocationCount)
        }
}
