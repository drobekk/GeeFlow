package app.geeflow.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.domain.user.model.AppearanceSettings
import app.geeflow.domain.user.usecase.GetAppearanceSettingsUseCase
import org.koin.compose.koinInject
import app.geeflow.data.user.model.ThemeMode as UserThemeMode

/**
 * Observes the selected user's appearance settings, retaining the last known values across
 * activity recreation so the UI does not flash the defaults before the flow re-emits.
 */
@Composable
internal fun rememberAppearanceSettings(): State<AppearanceSettings> {
    val getAppearanceSettings = koinInject<GetAppearanceSettingsUseCase>()
    val appearanceState = rememberSaveable(stateSaver = AppearanceSettingsSaver) {
        mutableStateOf(AppearanceSettings())
    }
    var appearance by appearanceState

    LaunchedEffect(Unit) {
        getAppearanceSettings().collect { appearance = it }
    }

    return appearanceState
}

private val AppearanceSettingsSaver = listSaver<AppearanceSettings, Any>(
    save = {
        listOf(
            it.themeMode.name,
            it.appTheme.name,
            it.paletteStyle.name,
            it.customSeedColor,
            it.fullScreenMode,
            it.keepScreenOn,
        )
    },
    restore = {
        AppearanceSettings(
            themeMode = UserThemeMode.valueOf(it[0] as String),
            appTheme = AppTheme.valueOf(it[1] as String),
            paletteStyle = AppPaletteStyle.valueOf(it[2] as String),
            customSeedColor = it[3] as Int,
            fullScreenMode = it[4] as Boolean,
            keepScreenOn = it[5] as Boolean,
        )
    },
)
