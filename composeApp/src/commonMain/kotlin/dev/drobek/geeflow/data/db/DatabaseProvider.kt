package dev.drobek.geeflow.data.db

import app.cash.sqldelight.ColumnAdapter
import dev.drobek.geeflow.AppDatabase
import dev.drobek.geeflow.domain.brew.model.Condition
import dev.drobek.geeflow.domain.brew.model.ProfileMode
import dev.drobek.geeflow.domain.brew.model.ProfileStep
import devdrobekgeeflowdatadb.Brew_profiles
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton

private val conditionAdapter = object : ColumnAdapter<Condition, String> {
    override fun decode(databaseValue: String): Condition = Json.decodeFromString(databaseValue)
    override fun encode(value: Condition): String = Json.encodeToString(value)
}

private val stepsAdapter = object : ColumnAdapter<List<ProfileStep>, String> {
    override fun decode(databaseValue: String): List<ProfileStep> = Json.decodeFromString(databaseValue)
    override fun encode(value: List<ProfileStep>): String = Json.encodeToString(value)
}

private val profileModeAdapter = object : ColumnAdapter<ProfileMode, String> {
    override fun decode(databaseValue: String): ProfileMode = ProfileMode.valueOf(databaseValue)
    override fun encode(value: ProfileMode): String = value.name
}

private val positionAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int = databaseValue.toInt()
    override fun encode(value: Int): Long = value.toLong()
}

@Singleton
class DatabaseProvider(databaseDriverFactory: DatabaseDriverFactory) {
    val database = AppDatabase(
        driver = databaseDriverFactory.createDriver(),
        brew_profilesAdapter = Brew_profiles.Adapter(
            modeAdapter = profileModeAdapter,
            finishConditionAdapter = conditionAdapter,
            stepsAdapter = stepsAdapter,
            positionAdapter = positionAdapter
        )
    )
}
