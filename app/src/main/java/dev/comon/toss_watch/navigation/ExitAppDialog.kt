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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd
import dev.comon.toss_watch.R
import dev.comon.toss_watch.core.designsystem.component.BrandLogoCard
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme
import dev.comon.toss_watch.navigation.component.ExitNativeAd

/**
 * 최상단(BottomMenuRoute) 화면에서 시스템 뒤로가기 시 노출되는 앱 종료 확인 다이얼로그.
 * [text] 영역에는 AdMob 네이티브 광고([ExitNativeAd])를 노출한다.
 *
 * @param nativeAd 미리 로드해 둔 네이티브 광고. 아직 로드되지 않았거나 실패했으면 null이고,
 *   이때는 [BrandLogoCard]를 대신 보여 준다.
 */
@Composable
fun ExitAppDialog(
    nativeAd: NativeAd?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.exit_dialog_title),
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
                if (nativeAd != null) {
                    ExitNativeAd(nativeAd = nativeAd)
                } else {
                    BrandLogoCard(size = 96.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.exit_dialog_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.exit_dialog_cancel),
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
        ExitAppDialog(nativeAd = null, onConfirm = {}, onDismiss = {})
    }
}
