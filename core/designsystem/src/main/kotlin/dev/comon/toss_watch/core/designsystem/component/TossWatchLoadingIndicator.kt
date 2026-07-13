package dev.comon.toss_watch.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/**
 * 브랜드 공용 로딩 인디케이터.
 *
 * @param message 스피너 아래에 표시할 안내 문구 (null이면 스피너만 표시).
 */
@Composable
fun TossWatchLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4.dp,
        )
        message?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 화면 전체를 덮는 로딩 오버레이. 하단 콘텐츠 터치를 흡수해
 * 진행 중 중복 조작을 막는다.
 */
@Composable
fun TossWatchLoadingOverlay(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            TossWatchLoadingIndicator(message = message)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TossWatchLoadingIndicatorPreview() {
    TossWatchTheme {
        TossWatchLoadingIndicator(message = "로그인 중이에요…")
    }
}
