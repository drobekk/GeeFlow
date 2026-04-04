package dev.drobek.geeflow.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DeviceSettings(val deviceId: String, val entryPoint: EntryPoint? = null) : NavKey {
    @Serializable
    sealed interface EntryPoint {
        @Serializable
        object Brewing : EntryPoint
        @Serializable
        object Maintenance : EntryPoint
        @Serializable
        object Connectivity : EntryPoint
    }
}
