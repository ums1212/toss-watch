package dev.comon.toss_watch.feature.auth.presentation.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
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
import dev.comon.toss_watch.feature.auth.R
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
        onGuestLoginClick = { viewModel.handleIntent(AuthUiIntent.OnGuestLoginClicked) },
        onErrorDismiss = { viewModel.handleIntent(AuthUiIntent.OnAuthErrorDismissed) },
    )
}

@Composable
private fun LoginContent(
    uiState: AuthUiState,
    onGoogleLoginClick: () -> Unit,
    onGuestLoginClick: () -> Unit,
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
                text = stringResource(id = R.string.auth_login_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            GoogleSignInButton(
                text = stringResource(id = R.string.auth_google_sign_in),
                onClick = onGoogleLoginClick,
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(TossSpacing.stackMd))

            TextButton(onClick = onGuestLoginClick, enabled = !uiState.isLoading) {
                Text(
                    text = stringResource(id = R.string.auth_guest_login),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Text(
                text = stringResource(id = R.string.auth_guest_login_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(TossSpacing.sectionPadding))

            LoginFooter()

            Spacer(modifier = Modifier.height(TossSpacing.stackLg))
        }

        if (uiState.isLoading) {
            TossWatchLoadingOverlay(message = stringResource(id = R.string.auth_loading_message))
        }

        uiState.errorMessage?.let { message ->
            TossWatchErrorDialog(
                message = message,
                onDismiss = onErrorDismiss,
                title = stringResource(id = R.string.auth_error_dialog_title),
            )
        }
    }
}

/** 이용약관/개인정보처리방침 링크 및 저작권 표기. */
@Composable
private fun LoginFooter(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val termsUrl = stringResource(id = R.string.auth_footer_terms_url)
    val privacyUrl = stringResource(id = R.string.auth_footer_privacy_url)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(id = R.string.auth_footer_terms),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { uriHandler.openUri(termsUrl) },
            )
            Spacer(modifier = Modifier.width(TossSpacing.stackMd))
            Text(
                text = stringResource(id = R.string.auth_footer_privacy),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { uriHandler.openUri(privacyUrl) },
            )
        }

        Spacer(modifier = Modifier.height(TossSpacing.stackSm))

        Text(
            text = stringResource(id = R.string.auth_footer_copyright),
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
            onGuestLoginClick = {},
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
            onGuestLoginClick = {},
            onErrorDismiss = {},
        )
    }
}
