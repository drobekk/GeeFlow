package app.geeflow.data.brew.impl

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileMode
import app.geeflow.data.db.DatabaseProvider
import org.koin.core.annotation.Singleton

@Singleton
class BrewsDao(databaseProvider: DatabaseProvider) {
    private val dbQuery = databaseProvider.database.brewProfileQueries

    fun getAllBrewProfiles() = dbQuery.selectAll(::mapToBrewProfile).executeAsList()

    fun getBrewProfilesByUserId(userId: Long) =
        dbQuery.selectByUserId(userId, ::mapToBrewProfile).executeAsList()

    fun getBrewProfileById(id: Long) =
        dbQuery.selectById(id, ::mapToBrewProfile).executeAsOneOrNull()

    fun insertBrewProfile(brewProfile: BrewProfile) {
        dbQuery.insertBrewProfile(
            id = if (brewProfile.id == 0L) null else brewProfile.id,
            userId = brewProfile.userId,
            name = brewProfile.name,
            description = brewProfile.description,
            mode = brewProfile.mode.name,
            finishCondition = ConditionAdapter.encode(brewProfile.finishCondition),
            autoLinkOpen = if (brewProfile.autoLinkOpen) 1L else 0L,
            steps = ProfileStepsAdapter.encode(brewProfile.steps),
            position = brewProfile.position.toLong(),
        )
    }

    fun deleteBrewProfile(id: Long) {
        dbQuery.deleteById(id)
    }

    @Suppress("LongParameterList")
    private fun mapToBrewProfile(
        id: Long,
        userId: Long,
        name: String,
        description: String,
        mode: String,
        finishCondition: String,
        autoLinkOpen: Long,
        steps: String,
        position: Long,
    ): BrewProfile = BrewProfile(
        id = id,
        userId = userId,
        name = name,
        description = description,
        mode = ProfileMode.valueOf(mode),
        finishCondition = ConditionAdapter.decode(finishCondition),
        autoLinkOpen = autoLinkOpen != 0L,
        steps = ProfileStepsAdapter.decode(steps),
        position = position.toInt(),
    )
}
