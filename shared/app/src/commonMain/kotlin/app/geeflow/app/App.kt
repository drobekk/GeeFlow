package app.geeflow.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import app.geeflow.app.navigation.AppNavigator
import app.geeflow.app.navigation.GetInitialDestinationUseCase
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.domain.user.model.AppearanceSettings
import app.geeflow.domain.user.usecase.GetAppearanceSettingsUseCase
import app.geeflow.navigation.NavFeature
import app.geeflow.platform.FullScreenEffect
import app.geeflow.platform.KeepScreenOnEffect
import app.geeflow.platform.ThemeModeEffect
import app.geeflow.platform.getThemeProvider
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.ThemeMode
import app.geeflow.ui.theme.colorscheme.EmeraldSeed
import app.geeflow.ui.theme.colorscheme.EspressoSeed
import app.geeflow.ui.theme.colorscheme.RoseSeed
import app.geeflow.ui.theme.colorscheme.SapphireSeed
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import kotlinx.serialization.modules.plus
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.koinInject
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
        val getAppearanceSettings = koinInject<GetAppearanceSettingsUseCase>()
        val appearance by getAppearanceSettings().collectAsStateWithLifecycle(
            initialValue = AppearanceSettings(),
            minActiveState = Lifecycle.State.CREATED,
        )
        val darkMode = when (appearance.themeMode) {
            UserThemeMode.SYSTEM -> isSystemInDarkTheme()
            UserThemeMode.LIGHT -> false
            UserThemeMode.DARK -> true
        }

        GeeFlowTheme(
            darkMode = darkMode,
            colorScheme = getColorScheme(appearance, darkMode),
            modifier = Modifier.fillMaxSize(),
        ) {
            ThemeModeEffect(appearance.themeMode.mapToUiMode())
            KeepScreenOnEffect(appearance.keepScreenOn)
            FullScreenEffect(appearance.fullScreenMode)
            RootNavigation(closeApp)
        }
    }
}

private fun UserThemeMode.mapToUiMode() = when (this) {
    UserThemeMode.SYSTEM -> ThemeMode.System
    UserThemeMode.LIGHT -> ThemeMode.Light
    UserThemeMode.DARK -> ThemeMode.Dark
}

@Composable
private fun getColorScheme(
    appearance: AppearanceSettings,
    darkMode: Boolean,
): ColorScheme {
    val systemThemeProvider = remember { getThemeProvider() }
    val systemTheme = systemThemeProvider.getSystemColorScheme(darkMode)

    val seedColor = when (appearance.appTheme) {
        AppTheme.ESPRESSO -> EspressoSeed
        AppTheme.SAPPHIRE -> SapphireSeed
        AppTheme.EMERALD -> EmeraldSeed
        AppTheme.ROSE -> RoseSeed
        AppTheme.CUSTOM -> Color(appearance.customSeedColor)
        AppTheme.SYSTEM -> if (systemTheme != null) return systemTheme else EspressoSeed
    }

    return rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkMode,
        style = appearance.paletteStyle.toPaletteStyle(),
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )
}

private fun AppPaletteStyle.toPaletteStyle() = when (this) {
    AppPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
    AppPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
    AppPaletteStyle.VIBRANT -> PaletteStyle.Vibrant
    AppPaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun RootNavigation(closeApp: () -> Unit) {
    val getInitialDestinationUseCase = koinInject<GetInitialDestinationUseCase>()
    val navFeatures: List<NavFeature> = getKoin().getAll<NavFeature>()
    val initialDestination = remember { getInitialDestinationUseCase() }
    val backStack = rememberNavBackStack(navFeatures.savedStateConfig(), initialDestination)
    val navigator = remember { AppNavigator(finish = { closeApp() }, backStack = backStack) }
    val dialogSceneStrategy = remember { DialogSceneStrategy<NavKey>() }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        onBack = { backStack.removeLast() },
        sceneStrategies = listOf(listDetailStrategy, dialogSceneStrategy),
        backStack = backStack,
        predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
        entryProvider = entryProvider {
            navFeatures.forEach {
                with(it) { provideEntries(navigator) }
            }
        },
    )
}

private fun List<NavFeature>.savedStateConfig() = SavedStateConfiguration {
    serializersModule = this@savedStateConfig
        .map { it.serializerModule }
        .reduce { acc, module -> acc + module }
}
