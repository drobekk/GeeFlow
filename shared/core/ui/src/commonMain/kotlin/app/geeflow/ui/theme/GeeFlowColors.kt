package app.geeflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val waterLight = Color(0xFF1D5FBF)
private val waterVariantLight = Color(0xFF428DB6)

private val waterDark = Color(0xFF7291DE)
private val waterVariantDark = Color(0xFF79B3D7)
private val pressureLight = Color(0xFFB7442A)
private val pressureDark = Color(0xFFFF9C63)

@Immutable
data class Colors(
    val water: Color,
    val waterVariant: Color,
    val pressure: Color,
)

val lightCustomColors = Colors(
    water = waterLight,
    waterVariant = waterVariantLight,
    pressure = pressureLight,
)

val darkCustomColors = Colors(
    water = waterDark,
    waterVariant = waterVariantDark,
    pressure = pressureDark,
)

val LocalColors = staticCompositionLocalOf { lightCustomColors }

fun Color.disabled(alpha: Float = 0.38f): Color = this.copy(alpha = alpha)
