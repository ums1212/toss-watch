package dev.comon.toss_watch.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = White,
    primaryContainer = Blue80,
    onPrimaryContainer = Grey10,
    secondary = Grey40,
    onSecondary = White,
    background = Grey90,
    onBackground = Grey10,
    surface = White,
    onSurface = Grey10,
    surfaceVariant = Grey80,
    onSurfaceVariant = Grey40,
    outline = Grey60,
    error = Red40,
    onError = White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Grey10,
    primaryContainer = Blue40,
    onPrimaryContainer = White,
    secondary = Grey60,
    onSecondary = Grey10,
    background = Grey10,
    onBackground = Grey90,
    surface = Grey20,
    onSurface = Grey90,
    surfaceVariant = Grey40,
    onSurfaceVariant = Grey80,
    outline = Grey60,
    error = Red80,
    onError = Grey10,
)

/**
 * 전 모듈이 공유하는 Material 3 테마 루트.
 * 모든 feature 화면은 앱 셸에서 감싸는 이 테마를 통해서만
 * `MaterialTheme.colorScheme.*` / `typography` / `shapes` 토큰에 접근한다.
 *
 * @param dynamicColor Android 12+ 기기 벽지 기반 다이내믹 컬러 사용 여부.
 *   브랜드 아이덴티티 유지를 위해 기본값은 false이며, 설정 화면에서 토글 예정.
 */
@Composable
fun TossWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TossWatchTypography,
        shapes = TossWatchShapes,
        content = content,
    )
}
