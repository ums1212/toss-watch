package dev.comon.toss_watch.feature.tosskey.presentation.tosskey

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchButton
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.tosskey.presentation.TossKeyUiIntent
import dev.comon.toss_watch.feature.tosskey.presentation.TossKeyUiSideEffect
import dev.comon.toss_watch.feature.tosskey.presentation.TossKeyUiState
import dev.comon.toss_watch.feature.tosskey.presentation.TossKeyViewModel

/**
 * 토스증권 Open API 키(client_id/client_secret) 등록 화면.
 *
 * 최초 로그인 직후 온보딩 루트로도, 설정 화면에서 재등록 목적으로도 진입할 수 있다.
 * 등록에 성공하면 [onNavigateBack]을 호출한다 — 온보딩 루트에서는 :app 최상위 라우터가
 * 세션 스트림(토스 키 등록 여부) 변화를 감지해 대시보드로 전환하고, 설정 화면에서
 * 진입한 경우에는 실제로 이전 화면으로 복귀한다.
 *
 * @param onNavigateBack [TossKeyUiSideEffect.NavigateBack] 수신 시 호출.
 */
@Composable
fun TossKeyScreen(
    onNavigateBack: () -> Unit,
    viewModel: TossKeyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    TossKeyUiSideEffect.NavigateBack -> onNavigateBack()
                    is TossKeyUiSideEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    TossKeyContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TossKeyContent(
    uiState: TossKeyUiState,
    onIntent: (TossKeyUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "토스 API 키 등록") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(TossKeyUiIntent.OnBackClicked) }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = "토스증권 계좌 연동을 위해\n발급받은 Open API 키를 입력해 주세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = uiState.clientId,
                    onValueChange = { onIntent(TossKeyUiIntent.OnClientIdChanged(it)) },
                    label = { Text("클라이언트 ID") },
                    supportingText = { Text("예: tsck_live_...") },
                    singleLine = true,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.clientSecret,
                    onValueChange = { onIntent(TossKeyUiIntent.OnClientSecretChanged(it)) },
                    label = { Text("클라이언트 시크릿") },
                    supportingText = { Text("예: tssk_live_...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(28.dp))

                TossWatchButton(
                    text = if (uiState.isSaving) "등록 중이에요…" else "토스 API 키 등록",
                    onClick = { onIntent(TossKeyUiIntent.OnSubmit) },
                    enabled = !uiState.isSaving,
                )
            }

            uiState.errorMessage?.let { message ->
                TossWatchErrorDialog(
                    message = message,
                    onDismiss = { onIntent(TossKeyUiIntent.OnErrorDismissed) },
                    title = "등록에 실패했어요",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TossKeyContentPreview() {
    TossWatchTheme {
        TossKeyContent(
            uiState = TossKeyUiState(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TossKeyContentSavingPreview() {
    TossWatchTheme {
        TossKeyContent(
            uiState = TossKeyUiState(
                clientId = "tsck_live_example",
                clientSecret = "tssk_live_example",
                isSaving = true,
            ),
            onIntent = {},
        )
    }
}
