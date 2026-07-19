package dev.comon.watch_app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * :core:designsystem은 phone 쪽 androidx.compose.material3에 종속돼 있어
 * Wear Compose Material3(androidx.wear.compose.material3)에서 그대로 재사용할 수 없다.
 * 브랜드 일관성을 위해 동일한 색상 값만 이 모듈에 복제해 둔다.
 */
object WatchColors {
    val Blue40 = Color(0xFF1B64DA)
    val Red40 = Color(0xFFD22030)
    val White = Color(0xFFFFFFFF)
}

private val TosswatchColorScheme = ColorScheme(
    primary = WatchColors.Blue40,
    error = WatchColors.Red40,
)

@Composable
fun TosswatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TosswatchColorScheme,
        content = content,
    )
}
