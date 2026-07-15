package dev.comon.toss_watch.feature.dashboard.presentation.dashboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.feature.dashboard.domain.model.Account

/** 계좌목록 팝업 — 계좌를 선택하면 [onSelect]를 발행하고 다이얼로그를 닫는다. */
@Composable
fun AccountSelectDialog(
    accounts: List<Account>,
    selectedAccountSeq: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "계좌 선택",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                if (accounts.isEmpty()) {
                    Text(
                        text = "등록된 계좌가 없어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    accounts.forEach { account ->
                        AccountRow(
                            account = account,
                            isSelected = account.accountSeq == selectedAccountSeq,
                            onClick = { onSelect(account.accountSeq) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "닫기",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AccountRow(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .selectedBackground(isSelected)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.accountNo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = account.accountType,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "선택된 계좌",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Modifier.selectedBackground(isSelected: Boolean): Modifier =
    if (isSelected) {
        this.background(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
        )
    } else {
        this
    }

@Preview(showBackground = true)
@Composable
private fun AccountSelectDialogPreview() {
    TossWatchTheme {
        AccountSelectDialog(
            accounts = listOf(
                Account(accountNo = "100012345678", accountSeq = 987654, accountType = "BROKERAGE"),
                Account(accountNo = "100098765432", accountSeq = 123456, accountType = "BROKERAGE"),
            ),
            selectedAccountSeq = 987654,
            onSelect = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountSelectDialogEmptyPreview() {
    TossWatchTheme {
        AccountSelectDialog(
            accounts = emptyList(),
            selectedAccountSeq = null,
            onSelect = {},
            onDismiss = {},
        )
    }
}
