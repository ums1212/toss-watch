package dev.comon.toss_watch.feature.tosskey.presentation

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.tosskey.domain.usecase.RegisterTossKeyUseCase
import dev.comon.toss_watch.feature.tosskey.util.FakeTossKeyRepository
import dev.comon.toss_watch.feature.tosskey.util.MainDispatcherRule
import dev.comon.toss_watch.feature.tosskey.util.TestDispatcherProvider
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
class TossKeyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeTossKeyRepository()

    private fun createViewModel(): TossKeyViewModel =
        TossKeyViewModel(
            registerTossKeyUseCase = RegisterTossKeyUseCase(fakeRepository),
            dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )

    private fun TestScope.collectSideEffects(
        viewModel: TossKeyViewModel,
    ): List<TossKeyUiSideEffect> {
        val effects = mutableListOf<TossKeyUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sideEffect.toList(effects)
        }
        return effects
    }

    @Test
    fun `등록 성공 시 UseCase가 호출되고 토스트와 NavigateBack이 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(TossKeyUiIntent.OnClientIdChanged("tsck_live_abc"))
            viewModel.handleIntent(TossKeyUiIntent.OnClientSecretChanged("tssk_live_def"))
            viewModel.handleIntent(TossKeyUiIntent.OnSubmit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertNull(state.errorMessage)
            assertEquals(1, fakeRepository.invocationCount)
            assertEquals("tsck_live_abc", fakeRepository.lastClientId)
            assertEquals("tssk_live_def", fakeRepository.lastClientSecret)
            assertEquals(
                listOf<TossKeyUiSideEffect>(
                    TossKeyUiSideEffect.ShowToast(TossKeyViewModel.TOAST_REGISTERED),
                    TossKeyUiSideEffect.NavigateBack,
                ),
                effects,
            )
        }

    @Test
    fun `등록 중에는 isSaving이 true이고 중복 제출은 무시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.suspendUntilReleased = true
            val viewModel = createViewModel()

            viewModel.handleIntent(TossKeyUiIntent.OnClientIdChanged("tsck_live_abc"))
            viewModel.handleIntent(TossKeyUiIntent.OnClientSecretChanged("tssk_live_def"))
            viewModel.handleIntent(TossKeyUiIntent.OnSubmit)
            runCurrent()
            viewModel.handleIntent(TossKeyUiIntent.OnSubmit)
            runCurrent()

            assertTrue(viewModel.uiState.value.isSaving)
            assertEquals(1, fakeRepository.invocationCount)

            fakeRepository.release()
            advanceUntilIdle()
        }

    @Test
    fun `필드가 비어 있으면 UseCase 호출 없이 에러 안내로 환원된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()

            viewModel.handleIntent(TossKeyUiIntent.OnClientIdChanged("tsck_live_abc"))
            viewModel.handleIntent(TossKeyUiIntent.OnSubmit)
            runCurrent()

            assertEquals(
                TossKeyViewModel.ERROR_EMPTY_FIELD,
                viewModel.uiState.value.errorMessage,
            )
            assertEquals(0, fakeRepository.invocationCount)
        }

    @Test
    fun `등록 API 에러 시 isSaving이 해제되고 errorMessage가 표시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.result = NetworkResult.ApiError(
                code = 400,
                message = "body에 'client_id'와 'client_secret'이 모두 필요합니다.",
            )
            val viewModel = createViewModel()

            viewModel.handleIntent(TossKeyUiIntent.OnClientIdChanged("tsck_live_abc"))
            viewModel.handleIntent(TossKeyUiIntent.OnClientSecretChanged("tssk_live_def"))
            viewModel.handleIntent(TossKeyUiIntent.OnSubmit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertEquals("body에 'client_id'와 'client_secret'이 모두 필요합니다.", state.errorMessage)
        }

    @Test
    fun `OnBackClicked는 NavigateBack 사이드이펙트를 발행한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(TossKeyUiIntent.OnBackClicked)
            runCurrent()

            assertEquals(
                listOf<TossKeyUiSideEffect>(TossKeyUiSideEffect.NavigateBack),
                effects,
            )
        }

    @Test
    fun `에러 다이얼로그 확인 시 errorMessage가 초기화된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()

            viewModel.handleIntent(TossKeyUiIntent.OnSubmit)
            runCurrent()
            assertEquals(TossKeyViewModel.ERROR_EMPTY_FIELD, viewModel.uiState.value.errorMessage)

            viewModel.handleIntent(TossKeyUiIntent.OnErrorDismissed)
            runCurrent()

            assertNull(viewModel.uiState.value.errorMessage)
        }
}
