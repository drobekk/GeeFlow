package dev.drobek.geeflow.app.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.drobek.geeflow.presentation.feature.device.registerDeviceSerializers
import dev.drobek.geeflow.presentation.feature.intro.registerIntroSerializers
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val destinationsSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            registerIntroSerializers()
            registerDeviceSerializers()
        }
    }
}
