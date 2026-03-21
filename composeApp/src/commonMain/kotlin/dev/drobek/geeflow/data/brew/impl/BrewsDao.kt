package dev.drobek.geeflow.data.brew.impl

import dev.drobek.geeflow.data.db.DatabaseProvider
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.brew.model.Condition
import dev.drobek.geeflow.domain.brew.model.ProfileMode
import dev.drobek.geeflow.domain.brew.model.ProfileStep
import org.koin.core.annotation.Singleton

@Singleton
class BrewsDao(databaseProvider: DatabaseProvider) {
    private val dbQuery = databaseProvider.database.brewProfileQueries

    fun getAllBrewProfiles() = dbQuery.selectAll(::mapToBrewProfile).executeAsList()

    fun getBrewProfilesByUserId(userId: Long) =
        dbQuery.selectByUserId(userId, ::mapToBrewProfile).executeAsList()

    fun getBrewProfileById(id: Long) =
        dbQuery.selectById(id, ::mapToBrewProfile).executeAsOneOrNull()

    fun getBrewProfileByDevice(deviceMac: String) =
        dbQuery.selectByDevice(deviceMac, ::mapToBrewProfile).executeAsOneOrNull()

    fun insertBrewProfile(brewProfile: BrewProfile) {
        dbQuery.insertBrewProfile(
            id = if (brewProfile.id == 0L) null else brewProfile.id,
            userId = brewProfile.userId,
            name = brewProfile.name,
            description = brewProfile.description,
            boundDeviceMac = brewProfile.boundDeviceMac,
            mode = brewProfile.mode,
            finishCondition = brewProfile.finishCondition,
            autoLinkOpen = brewProfile.autoLinkOpen,
            steps = brewProfile.steps
        )
    }

    fun deleteBrewProfile(id: Long) {
        dbQuery.deleteById(id)
    }

    fun bindProfile(deviceMac: String, profileId: Long) {
        dbQuery.transaction {
            dbQuery.unbindDevice(deviceMac)
            dbQuery.bindProfileToDevice(deviceMac, profileId)
        }
    }

    private fun mapToBrewProfile(
        id: Long,
        userId: Long,
        name: String,
        description: String,
        boundDeviceMac: String?,
        mode: ProfileMode,
        finishCondition: Condition,
        autoLinkOpen: Boolean,
        steps: List<ProfileStep>
    ): BrewProfile = BrewProfile(
        id = id,
        userId = userId,
        name = name,
        description = description,
        boundDeviceMac = boundDeviceMac,
        mode = mode,
        finishCondition = finishCondition,
        autoLinkOpen = autoLinkOpen,
        steps = steps
    )
}
