package app.geeflow.app.db

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.geeflow.data.brew.impl.BrewDataPointsAdapter
import app.geeflow.data.brew.impl.BrewHistoryDao
import app.geeflow.data.brew.impl.BrewHistoryRepositoryImpl
import app.geeflow.data.brew.impl.BrewProgramAdapter
import app.geeflow.data.brew.impl.BrewScaleSamplesAdapter
import app.geeflow.data.brew.impl.ConditionAdapter
import app.geeflow.data.brew.impl.FreeHandRecordingAdapter
import app.geeflow.data.brew.impl.ProfileExecutionTraceAdapter
import app.geeflow.data.brew.impl.ProfileStepsAdapter
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewShotMetadata
import appgeeflowdatabrewdb.Brew_history
import appgeeflowdatabrewdb.Brew_profiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class BrewHistoryDeviceScopeTest {
    @Test
    fun `shot metadata survives save-time races and history edits`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.use { driver ->
            val database = app.geeflow.data.brew.db.AppDatabase(
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
            app.geeflow.data.brew.db.AppDatabase.Schema.create(driver)
            driver.execute(null, "INSERT INTO users (id, name) VALUES (7, 'Ada')", 0)
            val history = BrewHistoryRepositoryImpl(BrewHistoryDao(database))
            val start = Instant.fromEpochMilliseconds(1_000)

            // The user edits Grind while the completed history row is still saving.
            history.saveShotMetadata(
                BrewShotMetadata(
                    deviceId = 11,
                    startedAt = start,
                    doseGrams = 18f,
                    grindSetting = "21",
                    recipeRatio = 2f,
                    recipeTargetGrams = 36f,
                    updatedAtEpochMillis = 30_001,
                ),
            )
            history.addBrew(
                entry(deviceId = 11, startedAt = 1_000).copy(
                    doseGrams = 18f,
                    grindSetting = "20",
                    endedAt = Instant.fromEpochMilliseconds(30_000),
                ),
                emptyMap(),
            )

            var saved = history.getBrewsForDevice(7, 11, "", 20, 0).single()
            assertEquals("21", saved.grindSetting)
            assertEquals(18f, saved.doseGrams)
            assertEquals(2f, saved.recipeRatio)
            assertEquals(36f, saved.recipeTargetGrams)
            assertEquals(30_000L, saved.endedAt?.toEpochMilliseconds())

            // Clearing Grind in history and final-weight settling must preserve one another.
            history.updateShotGrindSetting(11, start, null, 40_000)
            history.saveShotMetadata(
                BrewShotMetadata(
                    deviceId = 11,
                    startedAt = start,
                    doseGrams = 18f,
                    grindSetting = null,
                    endedAt = saved.endedAt,
                    extractionStartMillis = 12_000,
                    extractionStartEstimated = false,
                    updatedAtEpochMillis = 40_001,
                ),
            )
            history.updateShotFinalWeight(11, start, 9.4f, 50_000)
            saved = history.getBrewsForDevice(7, 11, "", 20, 0).single()
            assertNull(saved.grindSetting)
            assertEquals(9.4f, saved.finalWeightGrams)
            assertEquals(2f, saved.recipeRatio)
            assertEquals(36f, saved.recipeTargetGrams)
            assertEquals(12_000L, saved.extractionStartMillis)
            history.updateShotGrindSetting(11, start, "5.7", 60_000)
            history.saveShotMetadata(BrewShotMetadata(deviceId = 11, startedAt = start,
                doseGrams = 18f, grindSetting = "stale", updatedAtEpochMillis = 60_001))
            saved = history.getBrewsForDevice(7, 11, "", 20, 0).single()
            assertEquals("5.7", saved.grindSetting)
            assertEquals(2f, saved.recipeRatio)
            assertEquals(36f, saved.recipeTargetGrams)
        }
    }

    @Test
    fun `device history query returns only brews recorded for that device`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.use { driver ->
            val database = app.geeflow.data.brew.db.AppDatabase(
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
            app.geeflow.data.brew.db.AppDatabase.Schema.create(driver)
            driver.execute(null, "INSERT INTO users (id, name) VALUES (7, 'Ada')", 0)
            val history = BrewHistoryRepositoryImpl(BrewHistoryDao(database))

            history.addBrew(entry(deviceId = 11, startedAt = 1_000), emptyMap())
            history.addBrew(entry(deviceId = 22, startedAt = 2_000), emptyMap())
            history.addBrew(entry(deviceId = null, startedAt = 3_000), emptyMap())

            assertEquals(listOf(11L), history.getBrewsForDevice(7, 11, "", 20, 0).map { it.deviceId })
            assertEquals(listOf(22L), history.getBrewsForDevice(7, 22, "", 20, 0).map { it.deviceId })
            assertEquals(3, history.getBrews(7, "", 20, 0).size)
        }
    }

    private fun entry(deviceId: Long?, startedAt: Long) = BrewHistoryEntry(
        userId = 7,
        deviceId = deviceId,
        profileName = "Adaptive v2",
        mode = BrewMode.Profile,
        startedAt = Instant.fromEpochMilliseconds(startedAt),
        durationSeconds = 30,
    )
}
