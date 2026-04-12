package dev.drobek.geeflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow

interface Navigator {
    fun navigate(navEvent: NavEvent)
    fun getCurrentDestination(): NavKey?
    fun getPreviousDestination(): NavKey?
    val isAtRoot: Boolean
}

@Composable
fun NavigatorEffect(
    navigator: Navigator,
    navEventFlow: Flow<NavEvent>,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navEventFlow.collect(navigator::navigate)
        }
    }
}
