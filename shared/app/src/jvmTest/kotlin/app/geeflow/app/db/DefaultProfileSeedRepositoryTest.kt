package app.geeflow.app.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.EnumColumnAdapter
import app.geeflow.data.brew.impl.BrewDataPointsAdapter
import app.geeflow.data.brew.impl.BrewProfileRepositoryImpl
import app.geeflow.data.brew.impl.BrewProgramAdapter
import app.geeflow.data.brew.impl.BrewScaleSamplesAdapter
import app.geeflow.data.brew.impl.BrewsDao
import app.geeflow.data.brew.impl.ConditionAdapter
import app.geeflow.data.brew.impl.FreeHandRecordingAdapter
import app.geeflow.data.brew.impl.ProfileExecutionTraceAdapter
import app.geeflow.data.brew.impl.ProfileStepsAdapter
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.db.AppDatabase as BrewDatabase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.brew.provider.DefaultBrewProfileProvider
import appgeeflowdatabrewdb.Brew_history
import appgeeflowdatabrewdb.Brew_profiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class DefaultProfileSeedRepositoryTest {
    @Test
    fun `existing user receives the adapted starters once and taper values survive database storage`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.use { databaseDriver ->
            val database = newDatabase(databaseDriver)
            val dao = BrewsDao(database)
            dao.insertBrewProfile(
                BrewProfile(
                    userId = 1,
                    name = "My custom profile",
                    description = "",
                    finishCondition = Condition.Volume(40f),
                    steps = listOf(ProfileStep.Pressure(20, 6f)),
                ),
            )
            dao.markDefaultProfileSeeded(1, "scale-free-starters-v1")
            val repository = BrewProfileRepositoryImpl(dao, DefaultBrewProfileProvider())

            repository.seedDefaultProfilesIfEmpty(1)
            val firstRead = repository.getBrewProfilesForUser(1)
            val expectedNames = setOf(
                "My custom profile",
                "Classic 9 Bar + Preinfusion (No Scale)",
                "Adaptive v2 (LITA, No Scale)",
                "Damian's LRv2 (LITA, No Scale)",
                "LRv3-inspired 9 Bar Taper (No Scale)",
                "Hendon 6 Bar Decline (No Scale)",
            )
            assertEquals(expectedNames, firstRead.map { it.name }.toSet())
            val taper = assertNotNull(firstRead.singleOrNull { it.name == "LRv3-inspired 9 Bar Taper (No Scale)" })
            val phases = assertIs<BrewProgram.Phases>(taper.program).phases
            assertEquals(5.5f, (phases.last().control as PhaseControl.Pressure).bar)

            repository.seedDefaultProfilesIfEmpty(1)
            assertEquals(expectedNames, repository.getBrewProfilesForUser(1).map { it.name }.toSet())
        }
    }

    @Test
    fun `new user receives the complete default set including scale-free source profiles`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.use { databaseDriver ->
            val database = newDatabase(databaseDriver)
            val repository = BrewProfileRepositoryImpl(BrewsDao(database), DefaultBrewProfileProvider())

            repository.seedDefaultProfilesIfEmpty(2)

            val profiles = repository.getBrewProfilesForUser(2)
            assertEquals(17, profiles.size)
            assertEquals(
                setOf(
                    "Classic 9 Bar + Preinfusion (No Scale)",
                    "Adaptive v2 (LITA, No Scale)",
                    "Damian's LRv2 (LITA, No Scale)",
                    "LRv3-inspired 9 Bar Taper (No Scale)",
                    "Hendon 6 Bar Decline (No Scale)",
                ),
                profiles.take(5).map { it.name }.toSet(),
            )
        }
    }

    private fun newDatabase(driver: JdbcSqliteDriver): BrewDatabase {
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        val database = BrewDatabase(
            driver = driver,
            brew_profilesAdapter = Brew_profiles.Adapter(
                finishConditionAdapter = ConditionAdapter,
                programAdapter = BrewProgramAdapter,
            ),
            brew_historyAdapter = Brew_history.Adapter(
                modeAdapter = EnumColumnAdapter<BrewMode>(),
                profileStepsAdapter = ProfileStepsAdapter,
                profileRecordingAdapter = FreeHandRecordingAdapter,
                dataPointsAdapter = BrewDataPointsAdapter,
                executionTraceAdapter = ProfileExecutionTraceAdapter,
                scaleSamplesAdapter = BrewScaleSamplesAdapter,
                profileProgramAdapter = BrewProgramAdapter,
            ),
        )
        BrewDatabase.Schema.create(driver)
        driver.execute(null, "INSERT INTO users (id, name) VALUES (1, 'Existing'), (2, 'New')", 0)
        return database
    }

}
