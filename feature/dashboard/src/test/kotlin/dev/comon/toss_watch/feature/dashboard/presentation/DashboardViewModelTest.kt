package dev.comon.toss_watch.feature.dashboard.presentation

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchTargetTickersUseCase
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchUserAssetsUseCase
import dev.comon.toss_watch.feature.dashboard.util.FakeDashboardRepository
import dev.comon.toss_watch.feature.dashboard.util.MainDispatcherRule
import dev.comon.toss_watch.feature.dashboard.util.TestDispatcherProvider
import java.io.IOException
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeDashboardRepository()

    private fun createViewModel(): DashboardViewModel =
        DashboardViewModel(
            fetchUserAssetsUseCase = FetchUserAssetsUseCase(fakeRepository),
            fetchTargetTickersUseCase = FetchTargetTickersUseCase(fakeRepository),
            dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )

    private fun TestScope.collectSideEffects(
        viewModel: DashboardViewModel,
    ): List<DashboardUiSideEffect> {
        val effects = mutableListOf<DashboardUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sideEffect.toList(effects)
        }
        return effects
    }

    @Test
    fun `초기 로드 성공 시 자산 요약과 관찰 종목이 상태에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(FakeDashboardRepository.DEFAULT_ASSETS, state.totalAssets)
            assertEquals(FakeDashboardRepository.DEFAULT_TICKERS, state.activeTickers)
            assertNull(state.errorMessage)
        }

    @Test
    fun `OnSettingClicked Intent는 NavigateToSetting 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(DashboardUiIntent.OnSettingClicked)
            runCurrent()

            assertEquals(
                listOf<DashboardUiSideEffect>(DashboardUiSideEffect.NavigateToSetting),
                effects,
            )
        }

    @Test
    fun `ApiError 주입 시 크래시 없이 errorMessage로 환원되고 로딩이 종료된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.assetsResult =
                NetworkResult.ApiError(code = 500, message = "자산 집계 서버 오류")
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isRefreshing)
            assertEquals("자산 집계 서버 오류", state.errorMessage)
            // 실패한 자산 요약은 비어 있어도 성공한 종목 목록은 유지된다.
            assertNull(state.totalAssets)
            assertEquals(FakeDashboardRepository.DEFAULT_TICKERS, state.activeTickers)
        }

    @Test
    fun `메시지 없는 ApiError는 기본 에러 문구를 사용한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.tickersResult = NetworkResult.ApiError(code = 502, message = null)
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(
                DashboardViewModel.DEFAULT_API_ERROR,
                viewModel.uiState.value.errorMessage,
            )
        }

    @Test
    fun `NetworkError 시 네트워크 안내 문구가 표시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.assetsResult = NetworkResult.NetworkError(IOException("timeout"))
            fakeRepository.tickersResult = NetworkResult.NetworkError(IOException("timeout"))
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(DashboardViewModel.DEFAULT_NETWORK_ERROR, state.errorMessage)
        }

    @Test
    fun `OnRefreshTriggered는 isRefreshing 상태로 재조회한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.suspendUntilReleased = true
            viewModel.handleIntent(DashboardUiIntent.OnRefreshTriggered)
            runCurrent()

            assertTrue(viewModel.uiState.value.isRefreshing)
            assertFalse(viewModel.uiState.value.isLoading)

            fakeRepository.release()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(2, fakeRepository.assetsInvocationCount)
            assertEquals(2, fakeRepository.tickersInvocationCount)
        }

    @Test
    fun `로딩 중 중복 새로고침은 무시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.suspendUntilReleased = true
            val viewModel = createViewModel()
            runCurrent()

            viewModel.handleIntent(DashboardUiIntent.OnRefreshTriggered)
            runCurrent()

            assertEquals(1, fakeRepository.assetsInvocationCount)

            fakeRepository.release()
            advanceUntilIdle()
        }

    @Test
    fun `에러 다이얼로그 확인 시 errorMessage가 초기화된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.assetsResult = NetworkResult.ApiError(code = 500, message = "오류")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(DashboardUiIntent.OnErrorDismissed)
            runCurrent()

            assertNull(viewModel.uiState.value.errorMessage)
        }
}
