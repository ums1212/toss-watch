package dev.comon.toss_watch.feature.setting.presentation.watchpair

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchButton
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingIndicator
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.setting.R
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
        // shouldShowRequestPermissionRationale이 false인데 이미 한 번 이상 요청한 뒤라면
        // 사용자가 영구 거부(또는 2회 거부)한 것 — 시스템이 다시는 요청 다이얼로그를 띄우지 않는다.
        val permanentlyDenied = !granted && context.isCameraPermissionPermanentlyDenied()
        viewModel.handleIntent(WatchPairUiIntent.OnPermissionResult(granted, permanentlyDenied))
    }

    LaunchedEffect(Unit) {
        if (context.isCameraPermissionGranted()) {
            viewModel.handleIntent(WatchPairUiIntent.OnPermissionResult(true))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 앱 설정(권한 화면)에 다녀온 뒤 화면이 다시 보일 때(ON_RESUME) 권한 상태를 재확인한다.
    // 이 화면은 :app의 단일 Activity 내 Navigation 3 목적지라서, 같은 화면 안에서 이동할 때는
    // ON_RESUME이 재발생하지 않고 Activity가 백그라운드(설정 화면 등)에서 돌아올 때만 발생한다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = context.isCameraPermissionGranted()
                val permanentlyDenied = !granted && context.isCameraPermissionPermanentlyDenied()
                viewModel.handleIntent(WatchPairUiIntent.OnPermissionResult(granted, permanentlyDenied))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        onOpenAppSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null)),
            )
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.isCameraPermissionGranted(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.isCameraPermissionPermanentlyDenied(): Boolean =
    findActivity()?.let { activity ->
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    } == true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchPairContent(
    uiState: WatchPairUiState,
    onIntent: (WatchPairUiIntent) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.watchpair_top_bar_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(WatchPairUiIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.watchpair_back_desc),
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
                    text = stringResource(id = R.string.watchpair_scan_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(TossSpacing.stackLg),
                )

                if (uiState.isRegistering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        TossWatchLoadingIndicator(
                            message = stringResource(id = R.string.watchpair_registering),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = TossSpacing.containerMargin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(
                            id = if (uiState.isPermissionPermanentlyDenied) {
                                R.string.watchpair_permission_denied_permanent
                            } else {
                                R.string.watchpair_permission_needed
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    TossWatchButton(
                        text = stringResource(
                            id = if (uiState.isPermissionPermanentlyDenied) {
                                R.string.watchpair_open_settings_button
                            } else {
                                R.string.watchpair_grant_permission_button
                            },
                        ),
                        onClick = if (uiState.isPermissionPermanentlyDenied) onOpenAppSettings else onRequestPermission,
                        modifier = Modifier.padding(top = TossSpacing.containerMargin),
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(WatchPairUiIntent.OnRetry) },
                    title = stringResource(id = R.string.watchpair_error_dialog_title),
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
            onOpenAppSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchPairContentPermissionPermanentlyDeniedPreview() {
    TossWatchTheme {
        WatchPairContent(
            uiState = WatchPairUiState(
                hasCameraPermission = false,
                isPermissionPermanentlyDenied = true,
            ),
            onIntent = {},
            onRequestPermission = {},
            onOpenAppSettings = {},
        )
    }
}
