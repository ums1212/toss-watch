package dev.comon.watch_app.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dev.comon.toss_watch.core.common.coroutine.DispatcherProvider
import dev.comon.toss_watch.core.common.resources.StringProvider
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.watch_app.domain.repository.WatchPairingRepository
import dev.comon.watch_app.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchOnboardingStartupTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private var model: WatchOnboardingViewModel? = null

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        model?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun createModel(): WatchOnboardingViewModel = WatchOnboardingViewModel(
        GetOrCreateDeviceUuidUseCase(repository), GetFcmTokenUseCase(repository),
        CheckFcmTokenRegisteredUseCase(repository), IsPairedUseCase(repository),
        SavePairedStateUseCase(repository), QrCodeGenerator(),
        object : StringProvider {
            override fun getString(resId: Int) = "message"
            override fun getString(resId: Int, vararg formatArgs: Any) = "message"
        },
        object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val default = dispatcher
        },
    ).also { model = it }

    @Test fun qrIsVisibleWhileServerResponseIsPendingAndAfterFailure() = runTest(dispatcher) {
        val vm = createModel()
        runCurrent()
        assertEquals(1, repository.checks)
        assertTrue(vm.uiState.value.phase is WatchOnboardingPhase.Qr)
        repository.response.complete(NetworkResult.NetworkError(java.io.IOException("offline")))
        runCurrent()
        assertTrue(vm.uiState.value.phase is WatchOnboardingPhase.Qr)
        vm.viewModelScope.cancel()
    }

    @Test fun successfulPollingPersistsPairingBeforeStopping() = runTest(dispatcher) {
        val vm = createModel()
        runCurrent()
        repository.response.complete(NetworkResult.Success(true))
        runCurrent()
        assertTrue(repository.paired)
        assertTrue(vm.uiState.value.phase is WatchOnboardingPhase.Paired)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, repository.checks)
        vm.viewModelScope.cancel()
    }

    @Test fun bulkWrittenQrDecodesToOriginalPayload() {
        val payload = """{"fcmToken":"test-token","uuid":"test-uuid","modelName":"Samsung SM-R910"}"""
        val bitmap = QrCodeGenerator().generate(payload, 480)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        assertEquals(payload, QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text)
        bitmap.recycle()
    }

    private class FakeRepository : WatchPairingRepository {
        var paired = false
        var checks = 0
        val response = CompletableDeferred<NetworkResult<Boolean>>()
        override suspend fun getOrCreateDeviceUuid() = "test-uuid"
        override suspend fun getFcmToken() = Result.success("test-token")
        override suspend fun refreshFcmToken() = Result.success("new-test-token")
        override suspend fun isPaired() = paired
        override suspend fun setPaired(paired: Boolean) {
            yield() // Persistence must finish even when called by the polling job itself.
            this.paired = paired
        }
        override suspend fun checkFcmTokenRegistered(fcmToken: String): NetworkResult<Boolean> {
            checks++
            return response.await()
        }
    }
}
