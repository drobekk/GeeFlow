package dev.drobek.geeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class BaseViewModel<ViewState, Event>(initialState: ViewState) : ViewModel() {

    private val currentViewState = MutableStateFlow(initialState)
    val viewState: StateFlow<ViewState> = currentViewState.asStateFlow()

    private val eventsChannel = Channel<Event>()
    val events: Flow<Event> = eventsChannel.receiveAsFlow()

    protected fun emitEvent(event: Event) = launch {
        eventsChannel.send(event)
    }

    protected fun emitEvent(suspend: suspend () -> Event) = launch {
        eventsChannel.send(suspend())
    }

    protected fun modify(block: ViewState.() -> ViewState) {
        currentViewState.update(block::invoke)
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(block = block)

    protected fun launchCatching(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: Throwable) {
            ensureActive()
            onError(e)
        }
    }
}
