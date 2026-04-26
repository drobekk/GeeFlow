package app.geeflow.navigation

import androidx.navigation3.runtime.NavKey

sealed interface NavEvent {
    data class To(val key: NavKey) : NavEvent
    data class PopTo(val key: NavKey, val inclusive: Boolean = false) : NavEvent
    data class Remove(val key: NavKey) : NavEvent
    data object Back : NavEvent
    data object ClearBackStack : NavEvent
}
