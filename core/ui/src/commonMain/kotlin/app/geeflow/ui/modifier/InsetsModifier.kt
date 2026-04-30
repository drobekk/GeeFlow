package app.geeflow.ui.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.LayoutDirection

fun Modifier.geeFlowInsetsPadding() = composed {
    windowInsetsPadding(WindowInsets.geeFlowInsets)
}

@Composable
fun Modifier.geeFlowInsetsEndPadding(layoutDirection: LayoutDirection = LayoutDirection.Ltr): Modifier {
    val paddingEndInsets = WindowInsets.displayCutout.asPaddingValues().calculateEndPadding(layoutDirection)
    return this
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(end = paddingEndInsets)
}

@Composable
fun Modifier.geeFlowInsetsStartPadding(layoutDirection: LayoutDirection = LayoutDirection.Ltr): Modifier {
    val paddingStartInsets = WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(layoutDirection)
    return this
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(start = paddingStartInsets)
}

val WindowInsets.Companion.geeFlowInsets: WindowInsets
    @Composable
    get() = WindowInsets
        .statusBars
        .union(WindowInsets.navigationBars)
        .union(WindowInsets.displayCutout)
