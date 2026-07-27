package dev.comon.toss_watch.feature.setting.presentation.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.comon.toss_watch.core.designsystem.component.TossWatchButton
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.core.model.watch.PairedWatchInfo
import dev.comon.toss_watch.feature.setting.presentation.SettingUiIntent
import dev.comon.toss_watch.feature.setting.presentation.SettingUiSideEffect
import dev.comon.toss_watch.feature.setting.presentation.SettingUiState
import dev.comon.toss_watch.feature.setting.presentation.SettingViewModel

/**
 * 토스 연동 · Wear OS 연동 · 로그아웃 설정.
 *
 * @param onNavigateBack [SettingUiSideEffect.NavigateBack] 수신 시 호출.
 * @param onNavigateToTossKey [SettingUiSideEffect.NavigateToTossKey] 수신 시 호출.
 * @param onNavigateToWatchPair [SettingUiSideEffect.NavigateToWatchPair] 수신 시 호출.
 */
@Composable
fun SettingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTossKey: () -> Unit,
    onNavigateToWatchPair: () -> Unit,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    SettingUiSideEffect.NavigateBack -> onNavigateBack()
                    SettingUiSideEffect.NavigateToTossKey -> onNavigateToTossKey()
                    SettingUiSideEffect.NavigateToWatchPair -> onNavigateToWatchPair()
                }
            }
        }
    }

    SettingContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingContent(
    uiState: SettingUiState,
    onIntent: (SettingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "설정") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SettingUiIntent.OnBackClicked) }) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = TossSpacing.containerMargin,
                    vertical = TossSpacing.stackMd,
                ),
                verticalArrangement = Arrangement.spacedBy(TossSpacing.stackSm),
            ) {
                item(key = "toss_key_section") {
                    TossKeySection(onIntent = onIntent)
                }

                item(key = "watch_section") {
                    WatchTokenSection(pairedWatch = uiState.pairedWatch, onIntent = onIntent)
                }

                item(key = "logout_section") {
                    LogoutSection(onLogoutClicked = { showLogoutDialog = true })
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "로그아웃") },
            text = { Text(text = "로그아웃하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onIntent(SettingUiIntent.OnLogoutClicked)
                    },
                ) {
                    Text(text = "예")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "아니오")
                }
            },
        )
    }
}

@Composable
private fun TossKeySection(
    onIntent: (SettingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "토스 연동",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(TossSpacing.stackSm))

        TossWatchButton(
            text = "토스 API 키 재설정",
            onClick = { onIntent(SettingUiIntent.OnTossKeyClicked) },
        )
    }
}

@Composable
private fun WatchTokenSection(
    pairedWatch: PairedWatchInfo?,
    onIntent: (SettingUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = TossSpacing.sectionPadding)) {
        Text(
            text = "Wear OS 연동",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(TossSpacing.stackSm))

        if (pairedWatch != null) {
            Text(
                text = "연동된 워치: ${pairedWatch.modelName ?: "이름 미확인 워치"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "UUID: ${pairedWatch.uuid}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "워치 앱 페어링 화면에 표시된 QR 코드를 스캔해 연동해요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(TossSpacing.stackMd))

        TossWatchButton(
            text = if (pairedWatch != null) "재연동" else "QR로 워치 연동",
            onClick = { onIntent(SettingUiIntent.OnPairWatchClicked) },
        )
    }
}

@Composable
private fun LogoutSection(
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = TossSpacing.sectionPadding)) {
        OutlinedButton(
            onClick = onLogoutClicked,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "로그아웃")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingContentPreview() {
    TossWatchTheme {
        SettingContent(
            uiState = SettingUiState(),
            onIntent = {},
        )
    }
}
