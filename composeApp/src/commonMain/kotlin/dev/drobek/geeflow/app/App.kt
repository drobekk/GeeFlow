package dev.drobek.geeflow.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import kotlinx.serialization.modules.plus
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

@Composable
fun App(closeApp: () -> Unit) {
    KoinApplication(koinConfiguration<GeeFlowApp>()) {
        GeeFlowTheme {
            RootNavigation(closeApp)
        }
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
