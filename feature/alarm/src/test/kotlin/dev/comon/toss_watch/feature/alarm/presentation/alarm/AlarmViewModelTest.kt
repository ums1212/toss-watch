package dev.comon.toss_watch.feature.alarm.presentation.alarm

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.alarm.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.feature.alarm.domain.usecase.ObserveAlarmProfilesUseCase
import dev.comon.toss_watch.feature.alarm.util.FakeAlarmRepository
import dev.comon.toss_watch.feature.alarm.util.FakeStringProvider
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeAlarmRepository()

    private fun createViewModel(): AlarmViewModel =
        AlarmViewModel(
            fetchAlarmProfilesUseCase = FetchAlarmProfilesUseCase(fakeRepository),
            observeAlarmProfilesUseCase = ObserveAlarmProfilesUseCase(fakeRepository),
            stringProvider = FakeStringProvider(),
            dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )

    private fun TestScope.collectSideEffects(
        viewModel: AlarmViewModel,
    ): List<AlarmUiSideEffect> {
        val effects = mutableListOf<AlarmUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sideEffect.toList(effects)
        }
        return effects
    }

    @Test
    fun `초기 로드 성공 시 종목코드로 그룹핑된 알림 개수 요약이 상태에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(
                listOf(
                    StockAlarmSummary(stockCode = "035420", stockName = "NAVER", alarmCount = 1),
                    StockAlarmSummary(stockCode = "005930", stockName = "삼성전자", alarmCount = 1),
                ),
                state.stockAlarms,
            )
        }

    @Test
    fun `동일 종목에 여러 알림이 등록되면 개수가 합산된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.seedAlarms = FakeAlarmRepository.DEFAULT_ALARMS +
                FakeAlarmRepository.DEFAULT_ALARMS[0].copy(id = 99L, hour = 18)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val summary = viewModel.uiState.value.stockAlarms.first { it.stockCode == "005930" }
            assertEquals(2, summary.alarmCount)
        }

    @Test
    fun `조회 실패 시 errorMessage가 상태에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.refreshResult = NetworkResult.ApiError(code = 500, message = "서버 오류")

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `OnErrorDismissed는 errorMessage를 초기화한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.refreshResult = NetworkResult.ApiError(code = 500, message = "서버 오류")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(AlarmUiIntent.OnErrorDismissed)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `OnStockClicked는 해당 종목 정보를 담은 NavigateToAlarmDetail 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(AlarmUiIntent.OnStockClicked("005930", "삼성전자"))
            runCurrent()

            assertEquals(
                listOf<AlarmUiSideEffect>(
                    AlarmUiSideEffect.NavigateToAlarmDetail("005930", "삼성전자"),
                ),
                effects,
            )
        }
}
