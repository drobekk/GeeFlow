package dev.drobek.geeflow.core.presentation

import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.NavEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

interface NavEventProvider {
    val navEvent: Flow<NavEvent>

    fun navigateBack()
    fun navigateTo(navKey: NavKey)
    fun popTo(navKey: NavKey, inclusive: Boolean = false)
    fun navigate(event: NavEvent)
    suspend fun testNavigate(event: NavEvent)
}

class NavEventProviderImpl : NavEventProvider {
    private val navEventChannel = Channel<NavEvent>(Channel.BUFFERED)
    override val navEvent: Flow<NavEvent> = navEventChannel.receiveAsFlow()

    override fun navigateTo(navKey: NavKey) = navigate(NavEvent.To(navKey))
    override fun navigateBack() = navigate(NavEvent.Back)
    override fun navigate(event: NavEvent) {
        navEventChannel.trySend(event)
    }

    override suspend fun testNavigate(event: NavEvent) {
        navEventChannel.send(event)
    }


    override fun popTo(navKey: NavKey, inclusive: Boolean) = navigate(NavEvent.PopTo(navKey, inclusive))
}
