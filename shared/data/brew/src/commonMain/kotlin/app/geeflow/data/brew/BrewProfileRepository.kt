package app.geeflow.data.brew

import app.geeflow.data.brew.model.BrewProfile
import kotlinx.coroutines.flow.StateFlow

interface BrewProfileRepository {
    val brewProfiles: StateFlow<List<BrewProfile>>
    fun addBrewProfile(brewProfile: BrewProfile)
    fun getBrewProfilesForUser(userId: Long): List<BrewProfile>
    fun getBrewProfileById(id: Long): BrewProfile?
    fun removeBrewProfile(id: Long)
    fun updateBrewProfilesPositions(profiles: List<BrewProfile>)
    fun resetProfilesForUser(userId: Long)
    fun seedDefaultProfilesIfEmpty(userId: Long)
}
