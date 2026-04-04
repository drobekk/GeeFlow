package dev.drobek.geeflow.data.brew.impl

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.brew.provider.DefaultBrewProfileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

@Singleton
class BrewProfileRepositoryImpl(
    private val brewsDao: BrewsDao,
    private val defaultBrewProfileProvider: DefaultBrewProfileProvider
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
        if (brewsDao.getBrewProfilesByUserId(userId).isEmpty()) {
            addDefaultProfiles(userId)
        }
        return brewsDao.getBrewProfilesByUserId(userId)
    }

    override fun getBrewProfileById(id: Long): BrewProfile? {
        return brewsDao.getBrewProfileById(id)
    }

    private fun addDefaultProfiles(userId: Long) {
        defaultBrewProfileProvider.getDefaultProfiles(userId).forEach { defaultProfile ->
            addBrewProfile(defaultProfile)
        }
    }

    override fun removeBrewProfile(id: Long) {
        brewsDao.deleteBrewProfile(id)
        refresh()
    }

    override fun updateBrewProfilesPositions(profiles: List<BrewProfile>) {
        profiles.forEach { profile ->
            brewsDao.insertBrewProfile(profile)
        }
        refresh()
    }

    private fun refresh() {
        _brewProfiles.value = brewsDao.getAllBrewProfiles()
    }
}
