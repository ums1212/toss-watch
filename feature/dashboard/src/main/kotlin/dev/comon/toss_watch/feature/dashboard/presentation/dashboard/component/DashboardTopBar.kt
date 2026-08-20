package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.R
import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardUiIntent
import dev.comon.toss_watch.feature.dashboard.presentation.DashboardViewModel

/**
 * 대시보드/알림 두 탭이 공유하는 상단 앱바 — [dev.comon.toss_watch.navigation.BottomMenuScreen]의
 * `Scaffold(topBar)`에 배치되어 탭을 전환해도 항상 같은 자리에 고정된다.
 *
 * 기본 인자로 받는 [DashboardViewModel]은 `hiltViewModel()`을 통해 `BottomMenuRoute` NavEntry의
 * ViewModelStore에서 해석되므로, [dev.comon.toss_watch.feature.dashboard.presentation.dashboard.DashboardScreen]이
 * 사용하는 것과 동일한 인스턴스다 — 계좌 목록/선택 상태를 그대로 공유한다.
 *
 * 설정 아이콘은 [DashboardUiIntent]/side-effect를 거치지 않고 [onSettingClick]을 직접 호출한다.
 * `BaseMviViewModel.sideEffect`는 replay 없는 `MutableSharedFlow`라, 알림 탭에 있어 DashboardScreen이
 * 컴포지션에서 빠진 동안에는 구독자가 없어 이벤트가 유실되기 때문이다 — 이 탑바는 항상 떠 있으므로
 * 콜백 직결이 안전하다.
 *
 * 계좌 목록(📖) 아이콘은 대시보드/알림 두 탭 모두에서 노출된다 — 알림 탭에서도 계좌를 바꾸면
 * 대시보드 탭의 포트폴리오가 그대로 갱신되어 보인다(동일 [DashboardViewModel] 인스턴스를 공유하므로).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    onSettingClick: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardTopBarContent(
        accounts = uiState.accounts,
        selectedAccountSeq = uiState.selectedAccountSeq,
        onAccountSelected = { accountSeq ->
            viewModel.handleIntent(DashboardUiIntent.OnAccountSelected(accountSeq))
        },
        onSettingClick = onSettingClick,
        modifier = modifier,
        windowInsets = windowInsets,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBarContent(
    accounts: List<Account>,
    selectedAccountSeq: Long?,
    onAccountSelected: (Long) -> Unit,
    onSettingClick: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    var isAccountDialogVisible by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        windowInsets = windowInsets,
        title = {
            // 로고(25:28 원본 비율) + 타이틀 텍스트. 기본 TopAppBar 높이(64dp)를 넘지
            // 않도록 로고는 24dp 높이로, 텍스트는 titleLarge(22sp) 이하로 제한한다.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.toss_watch_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .height(24.dp)
                        .aspectRatio(25f / 28f),
                )
                Spacer(modifier = Modifier.width(TossSpacing.stackSm))
                Text(
                    text = stringResource(id = R.string.dashboard_top_bar_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
            }
        },
        actions = {
            IconButton(
                onClick = { isAccountDialogVisible = true },
            ) {
                Image(
                    painter = painterResource(id = R.drawable.book),
                    contentDescription = stringResource(id = R.string.dashboard_account_list_desc),
                    modifier = Modifier.size(24.dp),
                )
            }
            IconButton(
                onClick = onSettingClick,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(id = R.string.dashboard_setting_desc),
                )
            }
        },
    )

    if (isAccountDialogVisible) {
        AccountSelectDialog(
            accounts = accounts,
            selectedAccountSeq = selectedAccountSeq,
            onSelect = { accountSeq ->
                onAccountSelected(accountSeq)
                isAccountDialogVisible = false
            },
            onDismiss = { isAccountDialogVisible = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardTopBarContentPreview() {
    TossWatchTheme {
        DashboardTopBarContent(
            accounts = listOf(
                Account(accountNo = "100012345678", accountSeq = 987654, accountType = "BROKERAGE"),
            ),
            selectedAccountSeq = 987654,
            onAccountSelected = {},
            onSettingClick = {},
        )
    }
}
