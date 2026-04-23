package dev.drobek.geeflow.app

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
import dev.drobek.geeflow.app.navigation.AppNavigator
import dev.drobek.geeflow.app.navigation.GetInitialDestinationUseCase
import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.DarkMode
import dev.drobek.geeflow.domain.user.model.AppearanceSettings
import dev.drobek.geeflow.domain.user.usecase.GetAppearanceSettingsUseCase
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.platform.FullScreenEffect
import dev.drobek.geeflow.platform.KeepScreenOnEffect
import dev.drobek.geeflow.platform.ThemeModeEffect
import dev.drobek.geeflow.platform.getThemeProvider
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.colorscheme.espressoColorScheme
import dev.drobek.geeflow.ui.theme.colorscheme.monoColorScheme
import dev.drobek.geeflow.ui.theme.colorscheme.roseColorScheme
import kotlinx.serialization.modules.plus
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

@Composable
fun App(closeApp: () -> Unit) {
    KoinApplication(koinConfiguration<GeeFlowApp>()) {
        val getAppearanceSettings = koinInject<GetAppearanceSettingsUseCase>()
        val appearance by getAppearanceSettings().collectAsStateWithLifecycle(
            initialValue = AppearanceSettings(),
            minActiveState = Lifecycle.State.CREATED,
        )
        val darkMode = when (appearance.darkMode) {
            DarkMode.SYSTEM -> isSystemInDarkTheme()
            DarkMode.LIGHT -> false
            DarkMode.DARK -> true
        }

        GeeFlowTheme(
            darkMode = darkMode,
            colorScheme = getColorScheme(appearance, darkMode),
            modifier = Modifier.fillMaxSize(),
        ) {
            ThemeModeEffect(darkTheme = darkMode)
            KeepScreenOnEffect(appearance.keepScreenOn)
            FullScreenEffect(appearance.fullScreenMode)
            RootNavigation(closeApp)
        }
    }
}

@Composable
private fun getColorScheme(
    appearance: AppearanceSettings,
    darkMode: Boolean,
): ColorScheme {
    val systemThemeProvider = remember { getThemeProvider() }
    val systemTheme = systemThemeProvider.getSystemColorScheme(darkMode)

    return when (appearance.appTheme) {
        AppTheme.ESPRESSO -> espressoColorScheme(darkMode)
        AppTheme.MONO -> monoColorScheme(darkMode)
        AppTheme.ROSE -> roseColorScheme(darkMode)
        AppTheme.SYSTEM if systemTheme != null -> systemTheme
        else -> espressoColorScheme(darkMode)
    }
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
