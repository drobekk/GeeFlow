package dev.drobek.geeflow.data.brew.api

import dev.drobek.geeflow.domain.brew.model.BrewProfile
import kotlinx.coroutines.flow.StateFlow

interface BrewProfileRepository {
    val brewProfiles: StateFlow<List<BrewProfile>>
    fun addBrewProfile(brewProfile: BrewProfile)
    fun getBrewProfilesForUser(userId: Long): List<BrewProfile>
    fun getBrewProfileById(id: Long): BrewProfile?
    fun bindProfile(userId: Long, profileId: Long)
    fun removeBrewProfile(id: Long)
}
