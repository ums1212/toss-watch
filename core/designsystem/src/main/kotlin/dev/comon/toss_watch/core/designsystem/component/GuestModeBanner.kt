package dev.comon.toss_watch.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.R
import dev.comon.toss_watch.core.designsystem.theme.TossSpacing
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/**
 * 게스트(더미 데이터 체험) 모드에서 탭 콘텐츠 상단에 상시 노출하는 슬림 배너.
 *
 * 화면에 보이는 자산/알림 데이터가 실제 계좌와 무관한 샘플임을 명확히 알려, 실 금융 정보로
 * 오인되지 않게 한다.
 */
@Composable
fun GuestModeBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = TossSpacing.containerMargin, vertical = TossSpacing.stackSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(id = R.string.guest_mode_banner),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(start = TossSpacing.stackSm),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GuestModeBannerPreview() {
    TossWatchTheme {
        GuestModeBanner()
    }
}
