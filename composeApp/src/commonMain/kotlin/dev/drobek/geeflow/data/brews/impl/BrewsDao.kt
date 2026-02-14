package dev.drobek.geeflow.data.brews.impl

import dev.drobek.geeflow.data.db.DatabaseProvider
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import org.koin.core.annotation.Singleton

@Singleton
class BrewsDao(databaseProvider: DatabaseProvider) {
    private val dbQuery = databaseProvider.database.brewProfileQueries

    fun getAllBrewProfiles() = dbQuery.selectAll(::mapToBrewProfile).executeAsList()

    fun getBrewProfilesByUserId(userId: Long) =
        dbQuery.selectByUserId(userId, ::mapToBrewProfile).executeAsList()

    fun insertBrewProfile(brewProfile: BrewProfile) {
        dbQuery.insertBrewProfile(
            id = if (brewProfile.id == 0L) null else brewProfile.id,
            userId = brewProfile.userId,
            name = brewProfile.name
        )
    }

    fun deleteBrewProfile(id: Long) {
        dbQuery.deleteById(id)
    }

    private fun mapToBrewProfile(
        id: Long,
        userId: Long,
        name: String
    ): BrewProfile = BrewProfile(id, userId, name)
}
