package dev.drobek.geeflow.domain.device.model

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val macAddress: String,
    val name: String,
    val isLastUsed: Boolean = false,
    val manufacturer: String = "",
    val model: String = "",
    val version: String = "",
    val boundProfileId: Long? = null
)
