package dev.comon.toss_watch.feature.alarm.presentation.alarmdetail

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.usecase.AddAlarmProfileUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.DeleteAlarmProfileUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.ObserveAlarmProfilesUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.SetAlarmEnabledInCacheUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.ToggleAlarmProfileUseCase
import dev.comon.toss_watch.feature.alarm.util.FakeAlarmRepository
import dev.comon.toss_watch.feature.alarm.util.MainDispatcherRule
import dev.comon.toss_watch.feature.alarm.util.TestDispatcherProvider
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
class AlarmDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeAlarmRepository()

    private fun createViewModel(): AlarmDetailViewModel =
        AlarmDetailViewModel(
            fetchAlarmProfilesUseCase = FetchAlarmProfilesUseCase(fakeRepository),
            observeAlarmProfilesUseCase = ObserveAlarmProfilesUseCase(fakeRepository),
            addAlarmProfileUseCase = AddAlarmProfileUseCase(fakeRepository),
            toggleAlarmProfileUseCase = ToggleAlarmProfileUseCase(fakeRepository),
            setAlarmEnabledInCacheUseCase = SetAlarmEnabledInCacheUseCase(fakeRepository),
            deleteAlarmProfileUseCase = DeleteAlarmProfileUseCase(fakeRepository),
            dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )

    private fun TestScope.collectSideEffects(
        viewModel: AlarmDetailViewModel,
    ): List<AlarmDetailUiSideEffect> {
        val effects = mutableListOf<AlarmDetailUiSideEffect>()
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
            assertEquals(FakeAlarmRepository.DEFAULT_ALARMS, state.alarms)
        }

    @Test
    fun `OnAddAlarm 성공 시 새 프로필이 공유 캐시를 통해 목록에 추가되고 토스트가 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(
                AlarmDetailUiIntent.OnAddAlarm("000660", "SK하이닉스", 10, 15, listOf(0, 1, 2, 3, 4, 5)),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("000660", fakeRepository.lastAddedStockCode)
            assertEquals("SK하이닉스", fakeRepository.lastAddedStockName)
            assertEquals(10, fakeRepository.lastAddedHour)
            assertEquals(15, fakeRepository.lastAddedMinute)
            assertEquals(listOf(0, 1, 2, 3, 4, 5), fakeRepository.lastAddedDaysOfWeek)
            assertEquals(
                FakeAlarmRepository.DEFAULT_ALARMS + FakeAlarmRepository.ADDED_ALARM,
                state.alarms,
            )
            assertEquals(
                listOf<AlarmDetailUiSideEffect>(
                    AlarmDetailUiSideEffect.ShowToast(AlarmDetailViewModel.TOAST_ALARM_ADDED),
                ),
                effects,
            )
        }

    @Test
    fun `OnDeleteAlarm 성공 시 해당 프로필이 공유 캐시에서 제거되고 토스트가 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(AlarmDetailUiIntent.OnDeleteAlarm(alarmId = 2L))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2L, fakeRepository.lastDeletedId)
            assertEquals(1, fakeRepository.deleteInvocationCount)
            assertEquals(
                FakeAlarmRepository.DEFAULT_ALARMS.filterNot { it.id == 2L },
                state.alarms,
            )
            assertEquals(
                listOf<AlarmDetailUiSideEffect>(
                    AlarmDetailUiSideEffect.ShowToast(AlarmDetailViewModel.TOAST_ALARM_DELETED),
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

            viewModel.handleIntent(AlarmDetailUiIntent.OnDeleteAlarm(alarmId = 2L))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(FakeAlarmRepository.DEFAULT_ALARMS, state.alarms)
            assertEquals("삭제 서버 오류", state.errorMessage)
        }

    @Test
    fun `OnToggleAlarm은 API 응답을 기다리지 않고 즉시 공유 캐시를 낙관적으로 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(AlarmDetailUiIntent.OnToggleAlarm(alarmId = 2L, enabled = true))
            runCurrent() // 500ms 디바운스가 지나기 전 — 아직 API는 호출되지 않은 시점.

            assertTrue(viewModel.uiState.value.alarms.first { it.id == 2L }.isEnabled)
            assertEquals(0, fakeRepository.toggleInvocationCount)
        }

    @Test
    fun `동일 알람을 연타하면 마지막 값만 디바운스 후 한 번만 API에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(AlarmDetailUiIntent.OnToggleAlarm(alarmId = 2L, enabled = true))
            viewModel.handleIntent(AlarmDetailUiIntent.OnToggleAlarm(alarmId = 2L, enabled = false))
            viewModel.handleIntent(AlarmDetailUiIntent.OnToggleAlarm(alarmId = 2L, enabled = true))
            advanceUntilIdle()

            assertEquals(1, fakeRepository.toggleInvocationCount)
            assertEquals(true, fakeRepository.lastToggledEnabled)
            assertTrue(viewModel.uiState.value.alarms.first { it.id == 2L }.isEnabled)
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

            viewModel.handleIntent(AlarmDetailUiIntent.OnToggleAlarm(alarmId = 1L, enabled = false))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.alarms.first { it.id == 1L }.isEnabled)
            assertEquals("서버 오류", state.errorMessage)
        }

    @Test
    fun `OnBackClicked는 NavigateBack 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(AlarmDetailUiIntent.OnBackClicked)
            runCurrent()

            assertEquals(
                listOf<AlarmDetailUiSideEffect>(AlarmDetailUiSideEffect.NavigateBack),
                effects,
            )
        }
}
