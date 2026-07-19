package dev.comon.toss_watch.feature.setting.presentation.watchpair

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.usecase.RegisterWatchTokenUseCase
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
class WatchPairViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeSettingRepository()

    private fun createViewModel(): WatchPairViewModel =
        WatchPairViewModel(
            registerWatchTokenUseCase = RegisterWatchTokenUseCase(fakeRepository),
            dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )

    private fun TestScope.collectSideEffects(
        viewModel: WatchPairViewModel,
    ): List<WatchPairUiSideEffect> {
        val effects = mutableListOf<WatchPairUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sideEffect.toList(effects)
        }
        return effects
    }

    @Test
    fun `QR 스캔 성공 시 UseCase가 호출되고 isRegistering이 올바르게 전이된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            val effects = collectSideEffects(viewModel)

            fakeRepository.suspendUntilReleased = true
            viewModel.handleIntent(WatchPairUiIntent.OnQrScanned("wear-fcm-token"))
            runCurrent()

            // 등록 요청이 진행되는 동안 isRegistering = true
            assertTrue(viewModel.uiState.value.isRegistering)

            fakeRepository.release()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRegistering)
            assertEquals(null, state.errorMessage)
            assertEquals(1, fakeRepository.registerInvocationCount)
            assertEquals("wear-fcm-token", fakeRepository.lastRegisteredToken)
            assertEquals(
                listOf<WatchPairUiSideEffect>(
                    WatchPairUiSideEffect.ShowToast(WatchPairViewModel.TOAST_TOKEN_REGISTERED),
                    WatchPairUiSideEffect.NavigateBack,
                ),
                effects,
            )
        }

    @Test
    fun `등록 중 재스캔은 UseCase를 다시 호출하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()

            fakeRepository.suspendUntilReleased = true
            viewModel.handleIntent(WatchPairUiIntent.OnQrScanned("first-token"))
            runCurrent()
            viewModel.handleIntent(WatchPairUiIntent.OnQrScanned("second-token"))
            runCurrent()

            assertEquals(1, fakeRepository.registerInvocationCount)

            fakeRepository.release()
            advanceUntilIdle()
        }

    @Test
    fun `토큰 등록 API 에러 시 isRegistering이 해제되고 errorMessage가 표시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.tokenResult = NetworkResult.ApiError(code = 400, message = "잘못된 토큰")
            val viewModel = createViewModel()

            viewModel.handleIntent(WatchPairUiIntent.OnQrScanned("bad-token"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRegistering)
            assertEquals("잘못된 토큰", state.errorMessage)
        }

    @Test
    fun `OnRetry는 errorMessage를 초기화한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.tokenResult = NetworkResult.ApiError(code = 400, message = "잘못된 토큰")
            val viewModel = createViewModel()

            viewModel.handleIntent(WatchPairUiIntent.OnQrScanned("bad-token"))
            advanceUntilIdle()
            assertEquals("잘못된 토큰", viewModel.uiState.value.errorMessage)

            viewModel.handleIntent(WatchPairUiIntent.OnRetry)
            runCurrent()

            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `OnPermissionResult는 hasCameraPermission 상태를 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()

            viewModel.handleIntent(WatchPairUiIntent.OnPermissionResult(true))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasCameraPermission)

            viewModel.handleIntent(WatchPairUiIntent.OnPermissionResult(false))
            runCurrent()
            assertFalse(viewModel.uiState.value.hasCameraPermission)
        }

    @Test
    fun `OnBackClicked는 NavigateBack 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(WatchPairUiIntent.OnBackClicked)
            runCurrent()

            assertEquals(
                listOf<WatchPairUiSideEffect>(WatchPairUiSideEffect.NavigateBack),
                effects,
            )
        }
}
