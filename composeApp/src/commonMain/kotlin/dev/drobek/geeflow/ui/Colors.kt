package dev.drobek.geeflow.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFFD49734)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFE6D0AF)
private val OnPrimaryContainer = Color(0xFF33240D)
private val Secondary = Color(0xFF4B3629)
private val OnSecondary = Color(0xFFFFFFFF)
private val SecondaryContainer = Color(0xFFE6D1C5)
private val OnSecondaryContainer = Color(0xFF33251C)
private val Tertiary = Color(0xFF7D6352)
private val OnTertiary = Color(0xFFFFFFFF)
private val TertiaryContainer = Color(0xFFE6D6CD)
private val OnTertiaryContainer = Color(0xFF332822)
private val Error = Color(0xFFB3261E)
private val OnError = Color(0xFFFFFFFF)
private val ErrorContainer = Color(0xFFE6ACA9)
private val OnErrorContainer = Color(0xFF330B09)
private val Background = Color(0xFFfcfcfc)
private val OnBackground = Color(0xFF333231)
private val Surface = Color(0xFFfcfcfc)
private val OnSurface = Color(0xFF333231)
private val SurfaceVariant = Color(0xFFe6e1db)
private val OnSurfaceVariant = Color(0xFF666057)
private val Outline = Color(0xFF999082)

private val PrimaryDark = Color(0xFFE6C899)
private val OnPrimaryDark = Color(0xFF4C3613)
private val PrimaryContainerDark = Color(0xFF66481A)
private val OnPrimaryContainerDark = Color(0xFFE6D0AF)
private val SecondaryDark = Color(0xFFE6C9B7)
private val OnSecondaryDark = Color(0xFF4C372A)
private val SecondaryContainerDark = Color(0xFF664938)
private val OnSecondaryContainerDark = Color(0xFFE6D1C5)
private val TertiaryDark = Color(0xFFE6D0C3)
private val OnTertiaryDark = Color(0xFF4C3C32)
private val TertiaryContainerDark = Color(0xFF665143)
private val OnTertiaryContainerDark = Color(0xFFE6D6CD)
private val ErrorDark = Color(0xFFE69490)
private val OnErrorDark = Color(0xFF4C100D)
private val ErrorContainerDark = Color(0xFF661511)
private val OnErrorContainerDark = Color(0xFFE6ACA9)
private val BackgroundDark = Color(0xFF333231)
private val OnBackgroundDark = Color(0xFFe6e4e3)
private val SurfaceDark = Color(0xFF333231)
private val OnSurfaceDark = Color(0xFFe6e4e3)
private val SurfaceVariantDark = Color(0xFF666057)
private val OnSurfaceVariantDark = Color(0xFFe6e0d6)
private val OutlineDark = Color(0xFFb3aca2)

val lightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline
)

val darkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)
