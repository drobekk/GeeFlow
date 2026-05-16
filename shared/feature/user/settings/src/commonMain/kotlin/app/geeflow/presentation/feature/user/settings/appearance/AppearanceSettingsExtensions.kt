package app.geeflow.presentation.feature.user.settings.appearance

import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme_custom
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme_emerald
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme_espresso
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme_rose
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme_sapphire
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme_system
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_dark_mode_dark
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_dark_mode_light
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_dark_mode_system
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_palette_style_expressive
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_palette_style_neutral
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_palette_style_tonal_spot
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_palette_style_vibrant
import org.jetbrains.compose.resources.StringResource

internal val ThemeMode.titleRes: StringResource
    get() = when (this) {
        ThemeMode.SYSTEM -> Res.string.user_settings_appearance_dark_mode_system
        ThemeMode.LIGHT -> Res.string.user_settings_appearance_dark_mode_light
        ThemeMode.DARK -> Res.string.user_settings_appearance_dark_mode_dark
    }

internal val AppTheme.titleRes: StringResource
    get() = when (this) {
        AppTheme.ESPRESSO -> Res.string.user_settings_appearance_app_theme_espresso
        AppTheme.SAPPHIRE -> Res.string.user_settings_appearance_app_theme_sapphire
        AppTheme.EMERALD -> Res.string.user_settings_appearance_app_theme_emerald
        AppTheme.ROSE -> Res.string.user_settings_appearance_app_theme_rose
        AppTheme.CUSTOM -> Res.string.user_settings_appearance_app_theme_custom
        AppTheme.SYSTEM -> Res.string.user_settings_appearance_app_theme_system
    }

internal val AppPaletteStyle.titleRes: StringResource
    get() = when (this) {
        AppPaletteStyle.TONAL_SPOT -> Res.string.user_settings_appearance_palette_style_tonal_spot
        AppPaletteStyle.NEUTRAL -> Res.string.user_settings_appearance_palette_style_neutral
        AppPaletteStyle.VIBRANT -> Res.string.user_settings_appearance_palette_style_vibrant
        AppPaletteStyle.EXPRESSIVE -> Res.string.user_settings_appearance_palette_style_expressive
    }
