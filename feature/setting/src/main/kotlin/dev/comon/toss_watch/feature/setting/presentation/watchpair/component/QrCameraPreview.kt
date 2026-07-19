package dev.comon.toss_watch.feature.setting.presentation.watchpair.component

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * CameraX 프리뷰 + ML Kit 바코드 스캐너로 QR 코드를 인식한다.
 * 워치 온보딩 QR은 순수 FCM 토큰 문자열이라 별도 파싱 없이 [onQrDetected]로 그대로 전달한다.
 *
 * 최초 1건만 콜백하도록 내부에서 가드한다 — 등록이 끝나기 전까지 매 프레임 재호출되는 것을 막는다.
 * [resetSignal] 값이 바뀌면(예: 등록 실패 후 재시도) 카메라 재바인딩 없이 가드만 초기화해
 * 다시 스캔을 받아들인다.
 *
 * 내부적으로 CameraX의 [ExperimentalGetImage] opt-in API(`ImageProxy.image`)를 사용하지만
 * androidx.annotation.OptIn으로 이 함수 안에서 소비해, 호출부는 opt-in할 필요가 없다.
 */
@OptIn(markerClass = [ExperimentalGetImage::class])
@Composable
fun QrCameraPreview(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
    resetSignal: Any? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQrDetected by rememberUpdatedState(onQrDetected)
    val hasDetected = remember { AtomicBoolean(false) }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }

    LaunchedEffect(resetSignal) {
        hasDetected.set(false)
    }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            cameraProviderRef.get()?.unbindAll()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                            ) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage == null || hasDetected.get()) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees,
                                )

                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        val token = barcodes.firstNotNullOfOrNull { it.rawValue }
                                        if (token != null && hasDetected.compareAndSet(false, true)) {
                                            currentOnQrDetected(token)
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.w(TAG, "QR 인식 실패", it)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }.onSuccess {
                        cameraProviderRef.set(cameraProvider)
                    }.onFailure {
                        Log.e(TAG, "카메라 바인딩 실패", it)
                    }
                },
                ContextCompat.getMainExecutor(ctx),
            )

            previewView
        },
    )
}

private const val TAG = "QrCameraPreview"
