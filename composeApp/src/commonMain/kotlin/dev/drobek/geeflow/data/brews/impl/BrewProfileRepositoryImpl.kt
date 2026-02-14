package dev.drobek.geeflow.data.brews.impl

import dev.drobek.geeflow.data.brews.api.BrewProfileRepository
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

@Singleton
class BrewProfileRepositoryImpl(
    private val brewsDao: BrewsDao
) : BrewProfileRepository {

    private val _brewProfiles = MutableStateFlow<List<BrewProfile>>(emptyList())
    override val brewProfiles: StateFlow<List<BrewProfile>> = _brewProfiles.asStateFlow()

    init {
        refresh()
    }

    override fun addBrewProfile(brewProfile: BrewProfile) {
        brewsDao.insertBrewProfile(brewProfile)
        refresh()
    }

    override fun getBrewProfilesForUser(userId: Long): List<BrewProfile> {
        return brewsDao.getBrewProfilesByUserId(userId)
    }

    override fun removeBrewProfile(id: Long) {
        brewsDao.deleteBrewProfile(id)
        refresh()
    }

    private fun refresh() {
        _brewProfiles.value = brewsDao.getAllBrewProfiles()
    }
}
