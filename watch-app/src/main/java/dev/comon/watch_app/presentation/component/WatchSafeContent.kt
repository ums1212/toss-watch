package dev.comon.watch_app.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Keep the scroll viewport inside the display, including its corners on round watches.
 * A square inscribed in a circle needs (1 - 1 / sqrt(2)) / 2 = 14.65% per edge.
 * 15% leaves a small rounding margin. Scaffold insets alone do not provide this guarantee.
 */
@Composable
internal fun WatchSafeContent(content: @Composable BoxScope.() -> Unit) {
    val round = LocalConfiguration.current.isScreenRound
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (round) maxWidth * 0.15f else 8.dp,
                    vertical = if (round) maxHeight * 0.15f else 8.dp,
                )
                .clipToBounds(),
            content = content,
        )
    }
}
