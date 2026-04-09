package dev.drobek.geeflow.data.device.model

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val id: Long = 0L,
    val name: String,
    val connection: DeviceConnection,
    val isLastUsed: Boolean = false,
    val manufacturer: String = "",
    val model: String = "",
    val version: String = "",
    val boundProfileId: Long? = null,
)
