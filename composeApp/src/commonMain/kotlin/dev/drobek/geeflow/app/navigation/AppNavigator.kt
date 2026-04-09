package dev.drobek.geeflow.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.NavEvent
import dev.drobek.geeflow.navigation.Navigator

class AppNavigator(
    val finish: () -> Unit,
    val backStack: NavBackStack<NavKey>
) : Navigator {

    override val isAtRoot: Boolean
        get() = backStack.size == 1

    override fun navigate(navEvent: NavEvent) {
        when (navEvent) {
            is NavEvent.To -> backStack.add(navEvent.key)
            is NavEvent.Back -> back()
            is NavEvent.PopTo -> popTo(navEvent.key, navEvent.inclusive)
            is NavEvent.Remove -> backStack.removeAll { it == navEvent.key }
            is NavEvent.ClearBackStack -> backStack.clear()
        }
    }

    private fun popTo(navKey: NavKey, inclusive: Boolean = false) {
        val targetIndex = backStack.indexOfLast { it == navKey }
        if (targetIndex != -1) {
            val fromIndex = if (inclusive) targetIndex else targetIndex + 1
            while (backStack.size > fromIndex) {
                backStack.removeLastOrNull()
            }
        }
    }

    override fun getCurrentDestination(): NavKey? = backStack.lastOrNull()
    override fun getPreviousDestination(): NavKey? = backStack.getOrNull(backStack.size - 2)

    private fun back() {
        if (backStack.size == 1) finish() else backStack.removeLastOrNull()
    }
}
