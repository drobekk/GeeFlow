package app.geeflow.data.brew.impl

import app.geeflow.data.brew.db.AppDatabase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.SingleDoseSettings
import org.koin.core.annotation.Singleton

@Singleton
class BrewsDao(database: AppDatabase) {
    private val dbQuery = database.brewProfileQueries
    private val defaultProfileSeedQuery = database.defaultProfileSeedQueries

    fun getAllBrewProfiles() = dbQuery.selectAll(::mapToBrewProfile).executeAsList()

    fun getBrewProfilesByUserId(userId: Long) =
        dbQuery.selectByUserId(userId, ::mapToBrewProfile).executeAsList()

    fun getBrewProfileById(id: Long) =
        dbQuery.selectById(id, ::mapToBrewProfile).executeAsOneOrNull()

    fun hasDefaultProfileSeed(userId: Long, seedKey: String): Boolean =
        defaultProfileSeedQuery.selectDefaultProfileSeed(userId, seedKey).executeAsOne()

    fun markDefaultProfileSeeded(userId: Long, seedKey: String) {
        defaultProfileSeedQuery.insertDefaultProfileSeed(userId, seedKey)
    }

    fun insertBrewProfile(brewProfile: BrewProfile): Long {
        dbQuery.insertBrewProfile(
            id = if (brewProfile.id == 0L) null else brewProfile.id,
            userId = brewProfile.userId,
            name = brewProfile.name,
            description = brewProfile.description,
            finishCondition = brewProfile.finishCondition,
            autoLinkOpen = if (brewProfile.autoLinkOpen) 1L else 0L,
            singleDoseEnabled = if (brewProfile.singleDoseSettings.enabled) 1L else 0L,
            singleDoseGrindingSize = brewProfile.singleDoseSettings.grindingSize.toLong(),
            singleDoseGrindingSpeed = brewProfile.singleDoseSettings.grindingSpeed.toLong(),
            program = brewProfile.program,
            position = brewProfile.position.toLong(),
        )
        return dbQuery.lastInsertId().executeAsOne()
    }

    fun deleteBrewProfile(id: Long) {
        dbQuery.deleteById(id)
    }

    fun recordPendingDeletion(userId: Long, profileId: Long) {
        dbQuery.recordPendingDeletion(userId, profileId)
    }

    fun getPendingDeletions(userId: Long): List<Long> =
        dbQuery.selectPendingDeletionsByUserId(userId).executeAsList()

    fun clearPendingDeletions(userId: Long, profileIds: Set<Long>) {
        profileIds.forEach { profileId -> dbQuery.deletePendingDeletion(userId, profileId) }
    }

    @Suppress("LongParameterList")
    private fun mapToBrewProfile(
        id: Long,
        userId: Long,
        name: String,
        description: String,
        finishCondition: Condition?,
        autoLinkOpen: Long,
        singleDoseEnabled: Long,
        singleDoseGrindingSize: Long,
        singleDoseGrindingSpeed: Long,
        program: BrewProgram,
        position: Long,
    ): BrewProfile = BrewProfile(
        id = id,
        userId = userId,
        name = name,
        description = description,
        finishCondition = finishCondition,
        autoLinkOpen = autoLinkOpen != 0L,
        singleDoseSettings = SingleDoseSettings(
            enabled = singleDoseEnabled != 0L,
            grindingSize = singleDoseGrindingSize.toInt(),
            grindingSpeed = singleDoseGrindingSpeed.toInt(),
        ),
        program = program,
        position = position.toInt(),
    )
}
