package dev.drobek.geeflow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import dev.drobek.geeflow.app.navigation.AppNavigation
import dev.drobek.geeflow.app.navigation.GetInitialDestinationUseCase
import dev.drobek.geeflow.app.navigation.destinationsSavedStateConfiguration
import dev.drobek.geeflow.presentation.feature.device.deviceEntries
import dev.drobek.geeflow.presentation.feature.intro.introEntries
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun App(closeApp: () -> Unit) {
    KoinApplication(koinConfiguration()) {
        GeeFlowTheme {
            RootNavigation(closeApp)
        }
    }
}

@Composable
private fun RootNavigation(closeApp: () -> Unit) {
    val getInitialDestinationUseCase = koinInject<GetInitialDestinationUseCase>()
    val initialDestination = remember { getInitialDestinationUseCase() }
    val backStack = rememberNavBackStack(destinationsSavedStateConfiguration, initialDestination)
    val navigator = remember {
        AppNavigation(
            finish = { closeApp() },
            backStack = backStack
        )
    }
    val dialogSceneStrategy = remember { DialogSceneStrategy<NavKey>() }

    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { backStack.removeLast() },
        sceneStrategy = dialogSceneStrategy,
        backStack = backStack,
        entryProvider = entryProvider {
            introEntries(navigator)
            deviceEntries(navigator)
        }
    )
}

