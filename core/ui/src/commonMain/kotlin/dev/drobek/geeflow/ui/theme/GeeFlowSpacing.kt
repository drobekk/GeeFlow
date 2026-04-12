package dev.drobek.geeflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GeeFlowSpacing(
    val contentHorizontal: Dp,
    val contentVertical: Dp,
    val fabHorizontal: Dp,
    val fabVertical: Dp,
)

fun compactSpacing() = GeeFlowSpacing(
    contentHorizontal = 24.dp,
    contentVertical = 24.dp,
    fabHorizontal = 8.dp,
    fabVertical = 8.dp,
)

fun expandedSpacing() = GeeFlowSpacing(
    contentHorizontal = 48.dp,
    contentVertical = 24.dp,
    fabHorizontal = 32.dp,
    fabVertical = 16.dp,
)

val LocalSpacing = staticCompositionLocalOf { compactSpacing() }
