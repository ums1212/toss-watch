package dev.comon.toss_watch

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dev.comon.toss_watch.core.common.coroutine.DefaultDispatcherProvider
import dev.comon.toss_watch.core.common.resources.DefaultStringProvider
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.model.NetworkResult
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.domain.repository.SettingRepository
import dev.comon.toss_watch.feature.setting.domain.usecase.RegisterWatchTokenUseCase
import dev.comon.toss_watch.feature.setting.presentation.watchpair.WatchPairScreen
import dev.comon.toss_watch.feature.setting.presentation.watchpair.WatchPairUiIntent
import dev.comon.toss_watch.feature.setting.presentation.watchpair.WatchPairViewModel
import dev.comon.toss_watch.feature.setting.presentation.watchpair.component.QrCameraPreview
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/** Real on-device ML Kit decoding; registration is fake and never contacts the backend.
 * Bitmap input bypasses the emulator's virtual camera, which does not show a watch QR.
 */
class WatchPairingRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun grantCamera() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName, Manifest.permission.CAMERA,
        )
    }

    private fun decodeQr(raw: String): String {
        val matrix = QRCodeWriter().encode(raw, BarcodeFormat.QR_CODE, 480, 480)
        val pixels = IntArray(480 * 480) { i -> if (matrix[i % 480, i / 480]) Color.BLACK else Color.WHITE }
        val bitmap = Bitmap.createBitmap(pixels, 480, 480, Bitmap.Config.ARGB_8888)
        val scanner = BarcodeScanning.getClient()
        return try {
            Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
                .first().rawValue!!
        } finally {
            scanner.close()
            bitmap.recycle()
        }
    }

    @Test fun decodedQrRegistersAndLeavesCameraScreen() {
        grantCamera()
        val calls = AtomicInteger()
        val repository = object : SettingRepository {
            override suspend fun registerWatchToken(fcmToken: String, uuid: String, modelName: String): NetworkResult<Unit> {
                assertEquals("emulator-test-token", fcmToken)
                assertEquals("11111111-1111-1111-1111-111111111111", uuid)
                assertEquals("Wear emulator", modelName)
                calls.incrementAndGet()
                return NetworkResult.Success(Unit)
            }
            override fun observePairedWatch() = flowOf<PairedWatchInfo?>(null)
            override suspend fun syncPairedWatch() = NetworkResult.Success(Unit)
            override fun logout() = Unit
            override fun observeGuestMode() = flowOf(true)
        }
        val viewModel = WatchPairViewModel(
            RegisterWatchTokenUseCase(repository),
            DefaultStringProvider(InstrumentationRegistry.getInstrumentation().targetContext),
            DefaultDispatcherProvider(),
        )
        val visible = mutableStateOf(true)
        compose.setContent {
            TossWatchTheme {
                if (visible.value) WatchPairScreen(onNavigateBack = { visible.value = false }, viewModel = viewModel)
            }
        }
        val invalid = decodeQr("not-a-pairing-payload")
        compose.runOnIdle { viewModel.handleIntent(WatchPairUiIntent.OnQrScanned(invalid)) }
        compose.waitUntil(5_000) { viewModel.uiState.value.errorMessage != null }
        assertEquals(0, calls.get())
        compose.runOnIdle { viewModel.handleIntent(WatchPairUiIntent.OnRetry) }
        val valid = decodeQr("""{"fcm_token":"emulator-test-token","uuid":"11111111-1111-1111-1111-111111111111","model_name":"Wear emulator"}""")
        compose.runOnIdle { viewModel.handleIntent(WatchPairUiIntent.OnQrScanned(valid)) }
        compose.waitUntil(10_000) { !visible.value }
        compose.waitForIdle()
        assertEquals(1, calls.get())
        assertFalse(viewModel.uiState.value.isRegistering)
    }

    @Test fun cameraCanBeRemovedDuringInitializationAndOpenedAgain() {
        grantCamera()
        val visible = mutableStateOf(true)
        compose.setContent { if (visible.value) QrCameraPreview(onQrDetected = {}) }
        repeat(10) {
            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()
            compose.runOnIdle { visible.value = true }
            compose.waitForIdle()
        }
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
    }
}
