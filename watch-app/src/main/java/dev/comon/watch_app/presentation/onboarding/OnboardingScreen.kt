package dev.comon.watch_app.presentation.onboarding

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import dev.comon.watch_app.R

@Composable
fun OnboardingRoute(viewModel: WatchOnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    is WatchOnboardingUiSideEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onRetryClick = { viewModel.handleIntent(WatchOnboardingUiIntent.RetryClicked) },
        onRefreshClick = { viewModel.handleIntent(WatchOnboardingUiIntent.RefreshClicked) },
        onCheckNowClick = { viewModel.handleIntent(WatchOnboardingUiIntent.CheckNowClicked) },
        onGenerateQrClick = { viewModel.handleIntent(WatchOnboardingUiIntent.GenerateQrClicked) },
    )
}

@Composable
fun OnboardingScreen(
    uiState: WatchOnboardingUiState,
    onRetryClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCheckNowClick: () -> Unit,
    onGenerateQrClick: () -> Unit,
) {
    val phase = uiState.phase
    AppScaffold {
        val listState = rememberTransformingLazyColumnState()
        val transformationSpec = rememberTransformationSpec()
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text(
                            text = if (phase is WatchOnboardingPhase.Paired) {
                                stringResource(R.string.onboarding_paired_title)
                            } else {
                                stringResource(R.string.onboarding_title)
                            },
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (phase) {
                            is WatchOnboardingPhase.Qr -> QrCodeCard(phase.bitmap)
                            is WatchOnboardingPhase.Paired -> PairedInfo(phase)
                            is WatchOnboardingPhase.Error -> Text(
                                text = phase.message,
                                color = MaterialTheme.colorScheme.error,
                            )
                            WatchOnboardingPhase.Loading -> CircularProgressIndicator()
                        }
                    }
                }
                if (phase is WatchOnboardingPhase.Error) {
                    item {
                        Button(
                            onClick = onRetryClick,
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(stringResource(R.string.onboarding_retry))
                        }
                    }
                }
                if (phase is WatchOnboardingPhase.Qr) {
                    item {
                        Button(
                            onClick = onRefreshClick,
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(stringResource(R.string.onboarding_refresh))
                        }
                    }
                    item {
                        Button(
                            onClick = onCheckNowClick,
                            enabled = !uiState.isCheckingNow,
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            if (uiState.isCheckingNow) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.onboarding_check_now))
                            }
                        }
                    }
                }
                if (phase is WatchOnboardingPhase.Paired) {
                    item {
                        Button(
                            onClick = onGenerateQrClick,
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(stringResource(R.string.onboarding_generate_qr))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairedInfo(phase: WatchOnboardingPhase.Paired) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.onboarding_paired_model, phase.modelName),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.padding(top = 4.dp))
        Text(
            text = stringResource(R.string.onboarding_paired_uuid, phase.uuid),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QrCodeCard(bitmap: Bitmap) {
    // ScreenScaffold의 contentPadding이 이미 원형 세이프존을 계산해 주므로,
    // 그 안에서는 폭을 최대한 채워 스캔 인식률을 높인다(과거 0.55f는 지나치게 보수적이었음).
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction = 0.9f)
            .aspectRatio(1f)
            .background(color = Color.White, shape = RoundedCornerShape(8.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.onboarding_title),
        )
    }
}
