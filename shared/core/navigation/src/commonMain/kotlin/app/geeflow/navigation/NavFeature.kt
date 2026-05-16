package app.geeflow.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule

interface NavFeature {
    val serializerModule: SerializersModule
    fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator)
}
