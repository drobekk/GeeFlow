package dev.drobek.geeflow.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val primaryLight = Color(0xFF7F5610)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFDDB2)
private val onPrimaryContainerLight = Color(0xFF624000)
private val secondaryLight = Color(0xFF7F560F)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFFFDDB1)
private val onSecondaryContainerLight = Color(0xFF624000)
private val tertiaryLight = Color(0xFF48672E)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFC9EEA7)
private val onTertiaryContainerLight = Color(0xFF314E19)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFFF8F4)
private val onBackgroundLight = Color(0xFF201B13)
private val surfaceLight = Color(0xFFFFF8F3)
private val onSurfaceLight = Color(0xFF201B13)
private val surfaceVariantLight = Color(0xFFEFE0CF)
private val onSurfaceVariantLight = Color(0xFF4F4539)
private val outlineLight = Color(0xFF817567)
private val outlineVariantLight = Color(0xFFD3C4B4)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF353027)
private val inverseOnSurfaceLight = Color(0xFFFAEFE2)
private val inversePrimaryLight = Color(0xFFF4BD6E)
private val surfaceDimLight = Color(0xFFE3D8CC)
private val surfaceBrightLight = Color(0xFFFFF8F3)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFDF2E5)
private val surfaceContainerLight = Color(0xFFF8ECDF)
private val surfaceContainerHighLight = Color(0xFFF2E6D9)
private val surfaceContainerHighestLight = Color(0xFFECE1D4)

private val primaryDark = Color(0xFFF4BD6E)
private val onPrimaryDark = Color(0xFF452B00)
private val primaryContainerDark = Color(0xFF624000)
private val onPrimaryContainerDark = Color(0xFFFFDDB2)
private val secondaryDark = Color(0xFFF3BD6E)
private val onSecondaryDark = Color(0xFF442B00)
private val secondaryContainerDark = Color(0xFF624000)
private val onSecondaryContainerDark = Color(0xFFFFDDB1)
private val tertiaryDark = Color(0xFFAED18D)
private val onTertiaryDark = Color(0xFF1C3704)
private val tertiaryContainerDark = Color(0xFF314E19)
private val onTertiaryContainerDark = Color(0xFFC9EEA7)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF18120B)
private val onBackgroundDark = Color(0xFFEDE0D4)
private val surfaceDark = Color(0xFF17130B)
private val onSurfaceDark = Color(0xFFECE1D4)
private val surfaceVariantDark = Color(0xFF4F4539)
private val onSurfaceVariantDark = Color(0xFFD3C4B4)
private val outlineDark = Color(0xFF9B8F80)
private val outlineVariantDark = Color(0xFF4F4539)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFECE1D4)
private val inverseOnSurfaceDark = Color(0xFF353027)
private val inversePrimaryDark = Color(0xFF7F5610)
private val surfaceDimDark = Color(0xFF17130B)
private val surfaceBrightDark = Color(0xFF3E382F)
private val surfaceContainerLowestDark = Color(0xFF120E07)
private val surfaceContainerLowDark = Color(0xFF201B13)
private val surfaceContainerDark = Color(0xFF241F17)
private val surfaceContainerHighDark = Color(0xFF2F2921)
private val surfaceContainerHighestDark = Color(0xFF3A342B)

val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

fun Color.disabled(alpha: Float = 0.38f): Color = this.copy(alpha = alpha)
