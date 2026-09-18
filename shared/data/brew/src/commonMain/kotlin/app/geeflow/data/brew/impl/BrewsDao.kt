package app.geeflow.data.brew.impl

import app.geeflow.data.brew.db.AppDatabase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import org.koin.core.annotation.Singleton

@Singleton
class BrewsDao(database: AppDatabase) {
    private val dbQuery = database.brewProfileQueries

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
            finishCondition = brewProfile.finishCondition,
            autoLinkOpen = if (brewProfile.autoLinkOpen) 1L else 0L,
            program = brewProfile.program,
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
        finishCondition: Condition?,
        autoLinkOpen: Long,
        program: BrewProgram,
        position: Long,
    ): BrewProfile = BrewProfile(
        id = id,
        userId = userId,
        name = name,
        description = description,
        finishCondition = finishCondition,
        autoLinkOpen = autoLinkOpen != 0L,
        program = program,
        position = position.toInt(),
    )
}
