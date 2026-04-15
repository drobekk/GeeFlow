package dev.drobek.geeflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val waterLight = Color(0xFF3767D5)
private val waterVariantLight = Color(0xFF428DB6)

private val waterDark = Color(0xFF7291DE)
private val waterVariantDark = Color(0xFF79B3D7)

@Immutable
data class Colors(
    val water: Color,
    val waterVariant: Color,
)

val lightCustomColors = Colors(
    water = waterLight,
    waterVariant = waterVariantLight,
)

val darkCustomColors = Colors(
    water = waterDark,
    waterVariant = waterVariantDark,
)

val LocalColors = staticCompositionLocalOf { lightCustomColors }

fun Color.disabled(alpha: Float = 0.38f): Color = this.copy(alpha = alpha)
