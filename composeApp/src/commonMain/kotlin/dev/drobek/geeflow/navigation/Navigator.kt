package dev.drobek.geeflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow

interface Navigator {
    fun navigate(navEvent: NavEvent)
    fun getCurrentDestination(): NavKey?
    val isAtRoot: Boolean
}

@Composable
fun NavigatorEffect(
    navigator: Navigator,
    navEventFlow: Flow<NavEvent>
) = LaunchedEffect(navEventFlow) {
    navEventFlow.collect { event ->
        navigator.navigate(event)
    }
}
