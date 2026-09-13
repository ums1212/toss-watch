package dev.comon.watch_app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox

internal val LocalWatchForeground = compositionLocalOf { true }

/** One NavDisplay owns both entries, so their ViewModels and saved scroll state stay shared. */
internal class WatchSwipeSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T> =
        WatchSwipeScene(
            key = entries.last().contentKey,
            entries = entries.takeLast(2),
            previousEntries = entries.dropLast(1),
            onBack = onBack,
        )
}

private data class WatchSwipeScene<T : Any>(
    override val key: Any,
    override val entries: List<NavEntry<T>>,
    override val previousEntries: List<NavEntry<T>>,
    val onBack: () -> Unit,
) : Scene<T> {
    override val content: @Composable () -> Unit = {
        val foreground = entries.last()
        val background = entries.getOrNull(entries.lastIndex - 1)
        SwipeToDismissBox(
            onDismissed = onBack,
            userSwipeEnabled = background != null,
            contentKey = foreground.contentKey,
            backgroundKey = background?.contentKey ?: "watch-root-background",
        ) { isBackground ->
            val entry = if (isBackground) background else foreground
            if (entry != null) {
                CompositionLocalProvider(LocalWatchForeground provides !isBackground) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        entry.Content()
                    }
                }
            }
        }
    }
}
