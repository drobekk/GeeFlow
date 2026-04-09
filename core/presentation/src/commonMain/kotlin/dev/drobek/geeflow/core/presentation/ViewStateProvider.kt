package dev.drobek.geeflow.core.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ViewStateProvider<ViewState> {
    val viewState: StateFlow<ViewState>
    fun modify(block: ViewState.() -> ViewState)
}

class StateProviderImpl<ViewState>(initialState: ViewState) : ViewStateProvider<ViewState> {
    private val currentViewState = MutableStateFlow(initialState)
    override val viewState: StateFlow<ViewState> = currentViewState.asStateFlow()

    override fun modify(block: ViewState.() -> ViewState) {
        currentViewState.update(block::invoke)
    }
}
