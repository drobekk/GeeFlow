package dev.drobek.geeflow.domain.user.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long = 0,
    val name: String,
    val photoUri: String? = null,
    val isSelected: Boolean = false,
    val favoriteDeviceSerialNumber: String? = null
)
