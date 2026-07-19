package dev.comon.watch_app.presentation.onboarding

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    OnboardingScreen(
        uiState = uiState,
        onRetryClick = { viewModel.handleIntent(WatchOnboardingUiIntent.RetryClicked) },
    )
}

@Composable
fun OnboardingScreen(
    uiState: WatchOnboardingUiState,
    onRetryClick: () -> Unit,
) {
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
                        Text(text = stringResource(R.string.onboarding_title))
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
                        when {
                            uiState.qrBitmap != null -> QrCodeCard(uiState.qrBitmap)
                            uiState.errorMessage != null -> Text(
                                text = uiState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                            )
                            else -> CircularProgressIndicator()
                        }
                    }
                }
                if (uiState.errorMessage != null) {
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
            }
        }
    }
}

@Composable
private fun QrCodeCard(bitmap: Bitmap) {
    // 원형 디스플레이의 곡면 테두리에 잘리지 않도록 화면 폭의 절반 수준으로 QR 크기를 제한한다.
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction = 0.55f)
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
