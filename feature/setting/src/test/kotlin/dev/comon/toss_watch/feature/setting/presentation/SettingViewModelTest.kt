package dev.comon.toss_watch.feature.setting.presentation

import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.usecase.LogoutUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObserveGuestModeUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.ObservePairedWatchUseCase
import dev.comon.toss_watch.feature.setting.domain.usecase.SyncPairedWatchUseCase
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
            observePairedWatchUseCase = ObservePairedWatchUseCase(fakeRepository),
            syncPairedWatchUseCase = SyncPairedWatchUseCase(fakeRepository),
            observeGuestModeUseCase = ObserveGuestModeUseCase(fakeRepository),
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

    @Test
    fun `게스트 모드 스트림이 uiState isGuest에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.guestMode.value = true

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isGuest)
        }

    @Test
    fun `게스트 모드에서도 OnLogoutClicked는 동일하게 저장소의 logout을 호출한다`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            fakeRepository.guestMode.value = true
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.handleIntent(SettingUiIntent.OnLogoutClicked)
            advanceUntilIdle()

            assertEquals(1, fakeRepository.logoutInvocationCount)
        }
}
