package dev.comon.toss_watch.feature.setting.presentation.watchpair

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchButton
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingIndicator
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.setting.presentation.watchpair.component.QrCameraPreview

/**
 * 워치 온보딩 화면의 QR(FCM 토큰)을 카메라로 스캔해 자동으로 서버에 등록한다.
 *
 * @param onNavigateBack [WatchPairUiSideEffect.NavigateBack] 수신 시 호출.
 */
@Composable
fun WatchPairScreen(
    onNavigateBack: () -> Unit,
    viewModel: WatchPairViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.handleIntent(WatchPairUiIntent.OnPermissionResult(granted))
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            viewModel.handleIntent(WatchPairUiIntent.OnPermissionResult(true))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    WatchPairUiSideEffect.NavigateBack -> onNavigateBack()
                    is WatchPairUiSideEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    WatchPairContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchPairContent(
    uiState: WatchPairUiState,
    onIntent: (WatchPairUiIntent) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "워치 QR 스캔") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(WatchPairUiIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.hasCameraPermission) {
                QrCameraPreview(
                    onQrDetected = { token -> onIntent(WatchPairUiIntent.OnQrScanned(token)) },
                    // errorMessage가 null↔non-null로 바뀔 때마다 내부 "1회만 콜백" 가드를
                    // 초기화한다 — 등록 실패 후 재시도 시 다시 스캔을 받아들여야 한다.
                    resetSignal = uiState.errorMessage,
                    modifier = Modifier.fillMaxSize(),
                )

                Text(
                    text = "워치 화면에 표시된 QR 코드를 비춰 주세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                )

                if (uiState.isRegistering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        TossWatchLoadingIndicator(
                            message = "워치를 연동하고 있어요…",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "QR 코드를 스캔하려면\n카메라 권한이 필요해요.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    TossWatchButton(
                        text = "카메라 권한 허용",
                        onClick = onRequestPermission,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(WatchPairUiIntent.OnRetry) },
                    title = "워치 연동에 실패했어요",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchPairContentPermissionDeniedPreview() {
    TossWatchTheme {
        WatchPairContent(
            uiState = WatchPairUiState(hasCameraPermission = false),
            onIntent = {},
            onRequestPermission = {},
        )
    }
}
