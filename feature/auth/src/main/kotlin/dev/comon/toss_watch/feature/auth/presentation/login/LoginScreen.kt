package dev.comon.toss_watch.feature.auth.presentation.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.BrandLogoCard
import dev.comon.toss_watch.core.designsystem.component.GoogleSignInButton
import dev.comon.toss_watch.core.designsystem.component.TossWatchErrorDialog
import dev.comon.toss_watch.core.designsystem.component.TossWatchLoadingOverlay
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.auth.BuildConfig
import dev.comon.toss_watch.feature.auth.presentation.AuthUiIntent
import dev.comon.toss_watch.feature.auth.presentation.AuthUiSideEffect
import dev.comon.toss_watch.feature.auth.presentation.AuthUiState
import dev.comon.toss_watch.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.launch

/**
 * 구글 소셜 로그인 랜딩 화면.
 *
 * 로그인 성공 후 대시보드/토스키 입력 화면으로의 전환은 이 화면이 직접 트리거하지 않는다.
 * 토큰 영속 저장이 끝나면 :app의 MainViewModel.sessionState가 이를 감지해 최상위
 * 라우터가 루트를 교체하므로(단일 소스 오브 트루스), 여기서는 로딩 상태만 유지한다.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val credentialClient = remember(context) { GoogleCredentialClient(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    is AuthUiSideEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onGoogleLoginClick = {
            scope.launch {
                val result =
                    credentialClient.requestIdToken(BuildConfig.GOOGLE_AUTH_CLIENT_ID)
                when (result) {
                    is GoogleCredentialResult.Success ->
                        viewModel.handleIntent(AuthUiIntent.OnGoogleLoginClicked(result.idToken))

                    is GoogleCredentialResult.Failure ->
                        viewModel.handleIntent(AuthUiIntent.OnGoogleCredentialFailed(result.message))

                    GoogleCredentialResult.Cancelled -> Unit
                }
            }
        },
        onErrorDismiss = { viewModel.handleIntent(AuthUiIntent.OnAuthErrorDismissed) },
    )
}

@Composable
private fun LoginContent(
    uiState: AuthUiState,
    onGoogleLoginClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TossSpacing.containerMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1.4f))

            BrandLogoCard()

            Spacer(modifier = Modifier.height(TossSpacing.stackLg))

            Text(
                text = "내 손목 위의 주식 알림,\n구글 계정으로 바로 시작하세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            GoogleSignInButton(
                text = "Google 계정으로 로그인",
                onClick = onGoogleLoginClick,
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(TossSpacing.sectionPadding))

            LoginFooter()

            Spacer(modifier = Modifier.height(TossSpacing.stackLg))
        }

        if (uiState.isLoading) {
            TossWatchLoadingOverlay(message = "로그인 중이에요…")
        }

        uiState.errorMessage?.let { message ->
            TossWatchErrorDialog(
                message = message,
                onDismiss = onErrorDismiss,
                title = "로그인에 실패했어요",
            )
        }
    }
}

/** 이용약관/개인정보처리방침 및 저작권 표기. 별도 화면이 아직 없어 탐색 없이 텍스트만 노출한다. */
@Composable
private fun LoginFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            Text(
                text = "이용약관",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(TossSpacing.stackMd))
            Text(
                text = "개인정보처리방침",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(TossSpacing.stackSm))

        Text(
            text = "© 2026 toss-watch. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentPreview() {
    TossWatchTheme {
        LoginContent(
            uiState = AuthUiState(),
            onGoogleLoginClick = {},
            onErrorDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentLoadingPreview() {
    TossWatchTheme {
        LoginContent(
            uiState = AuthUiState(isLoading = true),
            onGoogleLoginClick = {},
            onErrorDismiss = {},
        )
    }
}
