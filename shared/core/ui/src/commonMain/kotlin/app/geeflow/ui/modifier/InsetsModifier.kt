package app.geeflow.ui.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.geeflow.ui.GeeFlowInsets

/** Applies and consumes the requested outer edges using the current layout direction. */
@Composable
fun Modifier.geeFlowInsetsPadding(
    sides: WindowInsetsSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical,
) = windowInsetsPadding(GeeFlowInsets.content.only(sides))

@Composable
fun Modifier.geeFlowInsetsEndPadding() = windowInsetsPadding(GeeFlowInsets.endPane)

@Composable
fun Modifier.geeFlowInsetsStartPadding() = windowInsetsPadding(GeeFlowInsets.startPane)

val WindowInsets.Companion.geeFlowInsets: WindowInsets
    @Composable get() = GeeFlowInsets.content
