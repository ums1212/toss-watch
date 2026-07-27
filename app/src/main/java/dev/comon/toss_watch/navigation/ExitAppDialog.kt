package dev.comon.toss_watch.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.component.BrandLogoCard
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/**
 * 최상단(BottomMenuRoute) 화면에서 시스템 뒤로가기 시 노출되는 앱 종료 확인 다이얼로그.
 * [text] 영역은 추후 애드몹 광고가 들어갈 자리 — 지금은 [BrandLogoCard]를 플레이스홀더로 노출한다.
 */
@Composable
fun ExitAppDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "앱을 종료하시겠습니까?",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TossSpacing.stackSm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // TODO: 애드몹 광고 영역 — 지금은 브랜드 로고 플레이스홀더.
                BrandLogoCard(size = 96.dp)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "종료",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "취소",
                    style = MaterialTheme.typography.labelLarge,
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
private fun ExitAppDialogPreview() {
    TossWatchTheme {
        ExitAppDialog(onConfirm = {}, onDismiss = {})
    }
}
