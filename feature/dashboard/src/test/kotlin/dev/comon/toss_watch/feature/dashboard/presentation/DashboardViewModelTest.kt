package dev.comon.toss_watch.feature.dashboard.presentation

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchAccountsUseCase
import dev.comon.toss_watch.feature.dashboard.domain.usecase.FetchPortfolioUseCase
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
            fetchAccountsUseCase = FetchAccountsUseCase(fakeRepository),
            fetchPortfolioUseCase = FetchPortfolioUseCase(fakeRepository),
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
    fun `초기 로드 성공 시 계좌목록과 첫 번째 계좌의 포트폴리오가 상태에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(FakeDashboardRepository.DEFAULT_ACCOUNTS, state.accounts)
            assertEquals(
                FakeDashboardRepository.DEFAULT_ACCOUNTS.first().accountSeq,
                state.selectedAccountSeq,
            )
            assertEquals(FakeDashboardRepository.DEFAULT_PORTFOLIO, state.portfolio)
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
    fun `OnAccountSelected는 계좌목록을 다시 조회하지 않고 선택한 계좌의 포트폴리오만 재조회한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val otherAccountSeq = FakeDashboardRepository.DEFAULT_ACCOUNTS[1].accountSeq
            val otherPortfolio = FakeDashboardRepository.DEFAULT_PORTFOLIO.copy(
                summary = FakeDashboardRepository.DEFAULT_PORTFOLIO.summary.copy(
                    totalEvaluationKrw = 999_000.0,
                ),
            )
            fakeRepository.portfolioResultByAccountSeq =
                mapOf(otherAccountSeq to NetworkResult.Success(otherPortfolio))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(DashboardUiIntent.OnAccountSelected(otherAccountSeq))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(otherAccountSeq, state.selectedAccountSeq)
            assertEquals(otherPortfolio, state.portfolio)
            assertEquals(1, fakeRepository.accountsInvocationCount)
            assertEquals(2, fakeRepository.portfolioInvocationCount)
        }

    @Test
    fun `계좌목록 조회 실패 시에도 기본계좌 포트폴리오는 조회되고 에러가 표시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.accountsResult =
                NetworkResult.ApiError(code = 500, message = "계좌 조회 서버 오류")
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("계좌 조회 서버 오류", state.errorMessage)
            assertTrue(state.accounts.isEmpty())
            assertNull(state.selectedAccountSeq)
            // 계좌목록이 없어도 서버가 기본계좌로 폴백하는 포트폴리오 조회는 성공한다.
            assertEquals(FakeDashboardRepository.DEFAULT_PORTFOLIO, state.portfolio)
        }

    @Test
    fun `메시지 없는 ApiError는 기본 에러 문구를 사용한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.portfolioResult = NetworkResult.ApiError(code = 502, message = null)
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
            fakeRepository.accountsResult = NetworkResult.NetworkError(IOException("timeout"))
            fakeRepository.portfolioResult = NetworkResult.NetworkError(IOException("timeout"))
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(DashboardViewModel.DEFAULT_NETWORK_ERROR, state.errorMessage)
            assertNull(state.portfolio)
        }

    @Test
    fun `OnRefreshTriggered는 isRefreshing 상태로 계좌목록과 포트폴리오를 재조회한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.suspendUntilReleased = true
            viewModel.handleIntent(DashboardUiIntent.OnRefreshTriggered)
            runCurrent()

            assertTrue(viewModel.uiState.value.isRefreshing)
            assertFalse(viewModel.uiState.value.isLoading)

            // 계좌목록 → 포트폴리오는 순차 호출이므로 두 단계를 각각 풀어준다.
            fakeRepository.release()
            runCurrent()
            fakeRepository.release()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(2, fakeRepository.accountsInvocationCount)
            assertEquals(2, fakeRepository.portfolioInvocationCount)
        }

    @Test
    fun `로딩 중 중복 새로고침은 무시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.suspendUntilReleased = true
            val viewModel = createViewModel()
            runCurrent()

            viewModel.handleIntent(DashboardUiIntent.OnRefreshTriggered)
            runCurrent()

            assertEquals(1, fakeRepository.accountsInvocationCount)

            fakeRepository.release()
            runCurrent()
            fakeRepository.release()
            advanceUntilIdle()
        }

    @Test
    fun `에러 다이얼로그 확인 시 errorMessage가 초기화된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.accountsResult =
                NetworkResult.ApiError(code = 500, message = "오류")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(DashboardUiIntent.OnErrorDismissed)
            runCurrent()

            assertNull(viewModel.uiState.value.errorMessage)
        }
}
