package dev.drobek.geeflow.data.brews.api

import dev.drobek.geeflow.domain.brew.model.BrewProfile
import kotlinx.coroutines.flow.StateFlow

interface BrewProfileRepository {
    val brewProfiles: StateFlow<List<BrewProfile>>
    fun addBrewProfile(brewProfile: BrewProfile)
    fun getBrewProfilesForUser(userId: Long): List<BrewProfile>
    fun removeBrewProfile(id: Long)
}
