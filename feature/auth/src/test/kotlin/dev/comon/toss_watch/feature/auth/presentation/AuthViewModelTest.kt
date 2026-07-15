package dev.comon.toss_watch.feature.auth.presentation

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.auth.domain.usecase.LoginWithGoogleUseCase
import dev.comon.toss_watch.feature.auth.util.FakeAuthRepository
import dev.comon.toss_watch.feature.auth.util.MainDispatcherRule
import dev.comon.toss_watch.feature.auth.util.TestDispatcherProvider
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
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeAuthRepository()

    private fun createViewModel(): AuthViewModel =
        AuthViewModel(
            loginWithGoogleUseCase = LoginWithGoogleUseCase(fakeRepository),
            dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
        )

    private fun TestScope.collectSideEffects(
        viewModel: AuthViewModel,
    ): List<AuthUiSideEffect> {
        val effects = mutableListOf<AuthUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sideEffect.toList(effects)
        }
        return effects
    }

    @Test
    fun `구글 로그인 Intent 처리 시 UseCase가 호출되고 로딩 상태로 전환된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.suspendUntilReleased = true
            val viewModel = createViewModel()

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            runCurrent()

            assertTrue(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
            assertEquals(1, fakeRepository.invocationCount)
            assertEquals(ID_TOKEN, fakeRepository.lastIdToken)

            fakeRepository.release()
            advanceUntilIdle()
        }

    @Test
    fun `로딩 중 중복 클릭은 무시되어 UseCase가 한 번만 호출된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.suspendUntilReleased = true
            val viewModel = createViewModel()

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            runCurrent()
            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            runCurrent()

            assertEquals(1, fakeRepository.invocationCount)

            fakeRepository.release()
            advanceUntilIdle()
        }

    @Test
    fun `로그인 성공 + 토스 키 등록됨이면 NavigateToDashboard 사이드이펙트가 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.result = NetworkResult.Success(
                FakeAuthRepository.DEFAULT_SESSION.copy(hasTossKey = true),
            )
            val viewModel = createViewModel()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isLoggedIn)
            assertNull(state.errorMessage)
            assertEquals(listOf<AuthUiSideEffect>(AuthUiSideEffect.NavigateToDashboard), effects)
        }

    @Test
    fun `로그인 성공 + 토스 키 미등록이면 NavigateToTossKeyInput 사이드이펙트가 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.result = NetworkResult.Success(
                FakeAuthRepository.DEFAULT_SESSION.copy(hasTossKey = false),
            )
            val viewModel = createViewModel()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isLoggedIn)
            assertNull(state.errorMessage)
            assertEquals(
                listOf<AuthUiSideEffect>(AuthUiSideEffect.NavigateToTossKeyInput),
                effects,
            )
        }

    @Test
    fun `API 에러 시 서버 메시지가 errorMessage에 반영되고 네비게이션은 발생하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.result = NetworkResult.ApiError(code = 401, message = "구글 토큰 검증 실패")
            val viewModel = createViewModel()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isLoggedIn)
            assertEquals("구글 토큰 검증 실패", state.errorMessage)
            assertTrue(effects.isEmpty())
        }

    @Test
    fun `API 에러에 메시지가 없으면 기본 에러 문구를 사용한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.result = NetworkResult.ApiError(code = 500, message = null)
            val viewModel = createViewModel()

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            advanceUntilIdle()

            assertEquals(AuthViewModel.DEFAULT_API_ERROR, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `네트워크 에러 시 네트워크 안내 문구가 errorMessage에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.result = NetworkResult.NetworkError(IOException("timeout"))
            val viewModel = createViewModel()

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isLoggedIn)
            assertEquals(AuthViewModel.DEFAULT_NETWORK_ERROR, state.errorMessage)
        }

    @Test
    fun `에러 다이얼로그 확인 시 errorMessage가 초기화된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.result = NetworkResult.ApiError(code = 400, message = "id_token 누락")
            val viewModel = createViewModel()

            viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(ID_TOKEN))
            advanceUntilIdle()
            assertEquals("id_token 누락", viewModel.uiState.value.errorMessage)

            viewModel.handleIntent(AuthUiIntent.OnAuthErrorDismissed)
            runCurrent()

            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `Credential Manager 실패 Intent는 errorMessage로 환원된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()

            viewModel.handleIntent(AuthUiIntent.OnGoogleCredentialFailed(message = null))
            runCurrent()

            assertEquals(
                AuthViewModel.DEFAULT_CREDENTIAL_ERROR,
                viewModel.uiState.value.errorMessage,
            )
            assertEquals(0, fakeRepository.invocationCount)
        }

    companion object {
        private const val ID_TOKEN = "google-id-token"
    }
}
