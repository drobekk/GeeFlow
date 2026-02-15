package dev.drobek.geeflow.domain.device.model

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val serialNumber: String,
    val name: String,
    val macAddress: String,
    val isLastUsed: Boolean = false
)
