package app.geeflow.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.geeflow.app.navigation.RootNavigation
import app.geeflow.app.theme.rememberAppColorScheme
import app.geeflow.app.theme.rememberAppearanceSettings
import app.geeflow.app.theme.toUiThemeMode
import app.geeflow.platform.FullScreenEffect
import app.geeflow.platform.ThemeModeEffect
import app.geeflow.ui.theme.GeeFlowTheme
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration
import app.geeflow.data.user.model.ThemeMode as UserThemeMode

@Composable
fun App(closeApp: () -> Unit) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { addPlatformFileSupport() }
            .build()
    }

    KoinApplication(koinConfiguration<GeeFlowApp>()) {
        val appearance by rememberAppearanceSettings()
        val darkMode = when (appearance.themeMode) {
            UserThemeMode.SYSTEM -> isSystemInDarkTheme()
            UserThemeMode.LIGHT -> false
            UserThemeMode.DARK -> true
        }

        GeeFlowTheme(
            darkMode = darkMode,
            colorScheme = rememberAppColorScheme(appearance, darkMode),
            modifier = Modifier.fillMaxSize(),
        ) {
            ThemeModeEffect(appearance.themeMode.toUiThemeMode())
            FullScreenEffect(appearance.fullScreenMode)
            RootNavigation(closeApp)
            ProfileExecutionEffect(appearance.keepScreenOn)
        }
    }
}
