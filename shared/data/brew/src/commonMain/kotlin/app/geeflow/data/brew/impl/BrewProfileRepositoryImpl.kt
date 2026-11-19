package app.geeflow.data.brew.impl

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.provider.DefaultBrewProfileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

@Singleton
class BrewProfileRepositoryImpl(
    private val brewsDao: BrewsDao,
    private val defaultBrewProfileProvider: DefaultBrewProfileProvider,
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

    override fun upsertSynchronizedProfile(brewProfile: BrewProfile): BrewProfile {
        val localId = brewsDao.insertBrewProfile(brewProfile)
        refresh()
        return brewProfile.copy(id = localId)
    }

    override fun removeSynchronizedProfile(id: Long) {
        brewsDao.deleteBrewProfile(id)
        refresh()
    }

    override fun pendingSyncDeletions(userId: Long): List<Long> = brewsDao.getPendingDeletions(userId)

    override fun clearPendingSyncDeletions(userId: Long, profileIds: Set<Long>) {
        brewsDao.clearPendingDeletions(userId, profileIds)
    }

    override fun getBrewProfilesForUser(userId: Long): List<BrewProfile> =
        brewsDao.getBrewProfilesByUserId(userId)

    override fun seedDefaultProfilesIfEmpty(userId: Long) {
        if (brewsDao.hasDefaultProfileSeed(userId, PROFILE_SEED)) return

        val existing = brewsDao.getBrewProfilesByUserId(userId)
        if (existing.isEmpty()) {
            addDefaultProfiles(userId)
        } else {
            val defaults = defaultBrewProfileProvider.getDefaultProfiles(userId)
            val existingNames = existing.mapTo(mutableSetOf()) { it.name }
            existing.forEach { profile ->
                val replacement = defaultBrewProfileProvider.standardizedDescription(profile.name, profile.description)
                if (replacement != null) addBrewProfile(profile.copy(description = replacement))
            }
            if (existingNames.none { it.startsWith("Adaptive v2", ignoreCase = true) }) {
                val adaptive = defaults.firstOrNull { it.name == ADAPTIVE_PROFILE_NAME }
                if (adaptive != null) {
                    val firstPosition = existing.minOfOrNull { it.position } ?: 0
                    addBrewProfile(adaptive.copy(position = firstPosition - 1))
                }
            }
        }
        brewsDao.markDefaultProfileSeeded(userId, PROFILE_SEED)
    }

    override fun getBrewProfileById(id: Long): BrewProfile? = brewsDao.getBrewProfileById(id)

    private fun addDefaultProfiles(userId: Long) {
        defaultBrewProfileProvider.getDefaultProfiles(userId).forEach { defaultProfile ->
            addBrewProfile(defaultProfile)
        }
    }

    override fun removeBrewProfile(id: Long) {
        getBrewProfileById(id)?.let { brewsDao.recordPendingDeletion(it.userId, id) }
        brewsDao.deleteBrewProfile(id)
        refresh()
    }

    override fun updateBrewProfilesPositions(profiles: List<BrewProfile>) {
        profiles.forEach { profile ->
            brewsDao.insertBrewProfile(profile)
        }
        refresh()
    }

    override fun resetProfilesForUser(userId: Long) {
        brewsDao.getBrewProfilesByUserId(userId).forEach { profile ->
            removeBrewProfile(profile.id)
        }
        addDefaultProfiles(userId)
    }

    private fun refresh() {
        _brewProfiles.value = brewsDao.getAllBrewProfiles()
    }

    private companion object {
        const val PROFILE_SEED = "standardized-descriptions-adaptive-v1"
        const val ADAPTIVE_PROFILE_NAME = "Adaptive v2"
    }
}
