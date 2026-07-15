package dev.comon.toss_watch.feature.setting.presentation

import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.feature.setting.domain.usecase.AddAlarmProfileUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.FetchAlarmProfilesUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.RegisterWatchTokenUseCase
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
import org.junit.Assert.assertNull
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
            registerWatchTokenUseCase = RegisterWatchTokenUseCase(fakeRepository),
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
    fun `토큰 등록 성공 시 UseCase가 호출되고 isSaving이 올바르게 전이된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(SettingUiIntent.OnFcmTokenChanged("wear-fcm-token"))
            runCurrent()

            fakeRepository.suspendUntilReleased = true
            viewModel.handleIntent(SettingUiIntent.OnFcmTokenSubmitted)
            runCurrent()

            // 등록 요청이 진행되는 동안 isSaving = true
            assertTrue(viewModel.uiState.value.isSaving)

            fakeRepository.release()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertNull(state.errorMessage)
            assertEquals(1, fakeRepository.registerInvocationCount)
            assertEquals("wear-fcm-token", fakeRepository.lastRegisteredToken)
            assertEquals(
                listOf<SettingUiSideEffect>(
                    SettingUiSideEffect.ShowToast(SettingViewModel.TOAST_TOKEN_REGISTERED),
                ),
                effects,
            )
        }

    @Test
    fun `빈 토큰 제출은 UseCase 호출 없이 에러 안내로 환원된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnFcmTokenSubmitted)
            runCurrent()

            assertEquals(
                SettingViewModel.ERROR_EMPTY_TOKEN,
                viewModel.uiState.value.errorMessage,
            )
            assertEquals(0, fakeRepository.registerInvocationCount)
        }

    @Test
    fun `토큰 등록 API 에러 시 isSaving이 해제되고 errorMessage가 표시된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.tokenResult = NetworkResult.ApiError(code = 400, message = "잘못된 토큰")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnFcmTokenChanged("bad-token"))
            viewModel.handleIntent(SettingUiIntent.OnFcmTokenSubmitted)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isSaving)
            assertEquals("잘못된 토큰", state.errorMessage)
        }

    @Test
    fun `OnAddAlarm 성공 시 새 프로필이 목록에 추가되고 토스트가 발행된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val effects = collectSideEffects(viewModel)

            viewModel.handleIntent(SettingUiIntent.OnAddAlarm("000660", 10, 15))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("000660", fakeRepository.lastAddedStockCode)
            assertEquals(10, fakeRepository.lastAddedHour)
            assertEquals(15, fakeRepository.lastAddedMinute)
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
    fun `OnWatchTokenReceived는 입력란이 비어 있을 때만 프리필한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnWatchTokenReceived("paired-token"))
            runCurrent()
            assertEquals("paired-token", viewModel.uiState.value.fcmTokenInput)

            // 사용자가 입력한 값은 라우트 파라미터로 덮어쓰지 않는다.
            viewModel.handleIntent(SettingUiIntent.OnFcmTokenChanged("user-edited"))
            viewModel.handleIntent(SettingUiIntent.OnWatchTokenReceived("paired-token-2"))
            runCurrent()
            assertEquals("user-edited", viewModel.uiState.value.fcmTokenInput)
        }
}
