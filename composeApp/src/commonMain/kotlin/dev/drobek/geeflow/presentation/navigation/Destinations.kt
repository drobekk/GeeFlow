package dev.drobek.geeflow.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

sealed interface Destinations : NavKey {
    @Serializable
    object Start : Destinations

    @Serializable
    object Devices : Destinations
}

val destinationsSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Destinations.Start::class, Destinations.Start.serializer())
            subclass(Destinations.Devices::class, Destinations.Devices.serializer())
        }
    }
}
