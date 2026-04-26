package app.geeflow.ui

import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_LARGE_LOWER_BOUND
import app.geeflow.platform.calculateWindowSizeClass

@Composable
fun isWidthExpanded(): Boolean = calculateWindowSizeClass().isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)

@Composable
fun isWidthLarge(): Boolean = calculateWindowSizeClass().isWidthAtLeastBreakpoint(WIDTH_DP_LARGE_LOWER_BOUND)

@Composable
fun isHeightCompact(): Boolean = !calculateWindowSizeClass().isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
