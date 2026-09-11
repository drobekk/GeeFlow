package app.geeflow.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import app.geeflow.platform.platformContentInsets

/** App policy, independent of inset consumption. IME is handled separately by the screen. */
object GeeFlowInsets {
    val content: WindowInsets
        @Composable get() = platformContentInsets()

    val top: WindowInsets
        @Composable get() = content.only(WindowInsetsSides.Top)

    val bottom: WindowInsets
        @Composable get() = content.only(WindowInsetsSides.Bottom)

    val startPane: WindowInsets
        @Composable get() = content.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)

    val endPane: WindowInsets
        @Composable get() = content.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
}
