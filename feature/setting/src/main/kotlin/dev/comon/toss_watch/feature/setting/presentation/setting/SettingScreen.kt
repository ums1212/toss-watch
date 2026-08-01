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
import androidx.compose.ui.res.stringResource
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
import dev.comon.toss_watch.feature.setting.R
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
                title = { Text(text = stringResource(id = R.string.setting_top_bar_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SettingUiIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.setting_back_desc),
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
                    LogoutSection(
                        isGuest = uiState.isGuest,
                        onLogoutClicked = { showLogoutDialog = true },
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = stringResource(
                        id = if (uiState.isGuest) R.string.setting_login_dialog_title else R.string.setting_logout_dialog_title,
                    ),
                )
            },
            text = {
                Text(
                    text = stringResource(
                        id = if (uiState.isGuest) R.string.setting_login_dialog_message else R.string.setting_logout_dialog_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onIntent(SettingUiIntent.OnLogoutClicked)
                    },
                ) {
                    Text(text = stringResource(id = R.string.setting_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = stringResource(id = R.string.setting_dialog_dismiss))
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
            text = stringResource(id = R.string.setting_toss_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(TossSpacing.stackSm))

        TossWatchButton(
            text = stringResource(id = R.string.setting_toss_key_reset_button),
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
            text = stringResource(id = R.string.setting_watch_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(TossSpacing.stackSm))

        if (pairedWatch != null) {
            Text(
                text = stringResource(
                    id = R.string.setting_watch_paired_format,
                    pairedWatch.modelName ?: stringResource(id = R.string.setting_watch_unknown_model),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.setting_watch_uuid_format, pairedWatch.uuid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(id = R.string.setting_watch_unpaired_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(TossSpacing.stackMd))

        TossWatchButton(
            text = stringResource(
                id = if (pairedWatch != null) R.string.setting_watch_repair_button else R.string.setting_watch_pair_button,
            ),
            onClick = { onIntent(SettingUiIntent.OnPairWatchClicked) },
        )
    }
}

/**
 * 로그아웃/게스트 로그인 전환 버튼.
 *
 * @param isGuest true면 게스트(더미 데이터 체험) 모드 — 위험을 뜻하는 error 색상 대신
 *   primary 색상으로 "로그인하기"를 노출한다. 실 로그인 상태에서는 기존 로그아웃 그대로.
 */
@Composable
private fun LogoutSection(
    isGuest: Boolean,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = TossSpacing.sectionPadding)) {
        val color = if (isGuest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        OutlinedButton(
            onClick = onLogoutClicked,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
            border = BorderStroke(1.dp, color),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    id = if (isGuest) R.string.setting_login_button else R.string.setting_logout_button,
                ),
            )
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

@Preview(showBackground = true)
@Composable
private fun SettingContentGuestPreview() {
    TossWatchTheme {
        SettingContent(
            uiState = SettingUiState(isGuest = true),
            onIntent = {},
        )
    }
}
