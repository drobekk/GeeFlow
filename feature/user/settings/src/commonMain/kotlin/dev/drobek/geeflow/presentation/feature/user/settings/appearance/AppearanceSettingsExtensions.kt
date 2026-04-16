package dev.drobek.geeflow.presentation.feature.user.settings.appearance

import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.DarkMode
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme_espresso
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme_mono
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme_rose
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme_system
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_dark_mode_dark
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_dark_mode_light
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_dark_mode_system
import org.jetbrains.compose.resources.StringResource

internal val DarkMode.titleRes: StringResource
    get() = when (this) {
        DarkMode.SYSTEM -> Res.string.user_settings_appearance_dark_mode_system
        DarkMode.LIGHT -> Res.string.user_settings_appearance_dark_mode_light
        DarkMode.DARK -> Res.string.user_settings_appearance_dark_mode_dark
    }

internal val AppTheme.titleRes: StringResource
    get() = when (this) {
        AppTheme.ESPRESSO -> Res.string.user_settings_appearance_app_theme_espresso
        AppTheme.MONO -> Res.string.user_settings_appearance_app_theme_mono
        AppTheme.ROSE -> Res.string.user_settings_appearance_app_theme_rose
        AppTheme.SYSTEM -> Res.string.user_settings_appearance_app_theme_system
    }
