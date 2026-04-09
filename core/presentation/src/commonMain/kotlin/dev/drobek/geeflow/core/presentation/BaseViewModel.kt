package dev.drobek.geeflow.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

open class BaseViewModel<ViewState, Event>(initialState: ViewState) : ViewModel(),
    ViewStateProvider<ViewState> by StateProviderImpl(initialState),
    EventProvider<Event> by EventProviderImpl(),
    NavEventProvider by NavEventProviderImpl()

fun ViewModel.launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(block = block)

fun ViewModel.launchCatching(
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

