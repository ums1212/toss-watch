package dev.comon.toss_watch.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/**
 * 공용 에러 알림 다이얼로그.
 * feature 화면은 UiState의 errorMessage가 non-null일 때 이 다이얼로그를 띄우고,
 * [onDismiss]에서 에러 해제 Intent를 발행한다.
 */
@Composable
fun TossWatchErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "문제가 발생했어요",
    confirmText: String = "확인",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = confirmText,
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

@Preview
@Composable
private fun TossWatchErrorDialogPreview() {
    TossWatchTheme {
        TossWatchErrorDialog(
            message = "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
            onDismiss = {},
        )
    }
}
