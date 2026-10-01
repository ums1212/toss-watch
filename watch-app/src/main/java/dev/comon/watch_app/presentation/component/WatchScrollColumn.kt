package dev.comon.watch_app.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ScreenScaffold

/**
 * Full-screen scrollable column for round and square watches.
 * The scroll viewport covers the whole display so content scrolls through the curved
 * top/bottom edges instead of being clipped to an inscribed rectangle. The screen-shaped
 * padding from [ScreenScaffold] is applied inside the scrolling content, so it only keeps
 * the first/last items off the edge at rest and short content still centers vertically
 * (fillMaxSize's min height survives verticalScroll).
 * Plain verticalScroll ignores the bezel/crown, so rotary input is wired up here.
 */
@Composable
internal fun WatchScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(RotaryScrollableDefaults.behavior(scrollState), focusRequester)
                .verticalScroll(scrollState)
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}
