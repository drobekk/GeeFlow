package dev.drobek.geeflow.ui

import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import dev.drobek.geeflow.platform.calculateWindowSizeClass

@Composable
fun isExpanded(): Boolean {
    return calculateWindowSizeClass().isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
}
