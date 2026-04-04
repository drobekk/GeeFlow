package dev.drobek.geeflow.core.presentation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

interface EventProvider<Event> {
    val events: Flow<Event>
    fun emitEvent(event: Event)
}

class EventProviderImpl<Event> : EventProvider<Event> {
    private val eventsChannel = Channel<Event>(Channel.BUFFERED)
    override val events: Flow<Event> = eventsChannel.receiveAsFlow()

    override fun emitEvent(event: Event) {
        eventsChannel.trySend(event)
    }
}
