package dev.drobek.geeflow.domain.brew.model

import kotlinx.serialization.Serializable

@Serializable
data class BrewProfile(
    val id: Long = 0,
    val userId: Long,
    val name: String
)
