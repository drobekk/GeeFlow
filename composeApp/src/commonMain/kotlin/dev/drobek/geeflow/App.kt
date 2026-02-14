package dev.drobek.geeflow

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.drobek.geeflow.presentation.feature.start.StartScreen
import dev.drobek.geeflow.presentation.navigation.Destinations
import dev.drobek.geeflow.presentation.navigation.destinationsSavedStateConfiguration
import dev.drobek.geeflow.ui.GeeFlowTheme
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
    KoinApplication(koinConfiguration()) {
        GeeFlowTheme {
            RootNavigation()
        }
    }
}

@Composable
private fun RootNavigation() {
    val backStack = rememberNavBackStack(destinationsSavedStateConfiguration, Destinations.Start)
    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { backStack.removeLast() },
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Destinations.Start>(content = { StartScreen(onClick = { backStack.add(Destinations.Devices) }) })
            entry<Destinations.Devices>(content = { Text("Devices list") })
        }
    )
}
