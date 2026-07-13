package dev.comon.toss_watch.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.comon.toss_watch.core.designsystem.theme.TossWatchTheme

/**
 * Toss Watch 표준 주요 액션 버튼 (Primary Filled).
 *
 * @param isLoading true면 라벨 대신 스피너를 표시하고 클릭을 차단한다.
 *   진행 중 재클릭으로 인한 중복 요청을 컴포넌트 차원에서 방지.
 * @param leadingIcon 라벨 왼쪽에 붙는 아이콘 슬롯 (예: 구글 로고).
 */
@Composable
fun TossWatchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.let { icon ->
                    icon()
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TossWatchButtonPreview() {
    TossWatchTheme {
        TossWatchButton(text = "Google 계정으로 로그인", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TossWatchButtonLoadingPreview() {
    TossWatchTheme {
        TossWatchButton(text = "Google 계정으로 로그인", onClick = {}, isLoading = true)
    }
}
