package app.geeflow.app.db

import app.cash.sqldelight.EnumColumnAdapter
import app.geeflow.data.brew.impl.BrewDataPointsAdapter
import app.geeflow.data.brew.impl.BrewProgramAdapter
import app.geeflow.data.brew.impl.ConditionAdapter
import app.geeflow.data.brew.impl.FreeHandRecordingAdapter
import app.geeflow.data.brew.impl.ProfileExecutionTraceAdapter
import app.geeflow.data.brew.impl.ProfileStepsAdapter
import app.geeflow.data.brew.model.BrewMode
import appgeeflowdatabrewdb.Brew_history
import appgeeflowdatabrewdb.Brew_profiles
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import app.geeflow.data.brew.db.AppDatabase as BrewDatabase
import app.geeflow.data.device.db.AppDatabase as DeviceDatabase
import app.geeflow.data.user.db.AppDatabase as UserDatabase

@Module
class AppDatabaseModule {
    @Single
    fun appDatabase(driverFactory: DatabaseDriverFactory): AppDatabase = AppDatabase(
        driver = driverFactory.createDriver(),
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
        ),
    )

    @Single
    fun userAppDatabase(appDatabase: AppDatabase): UserDatabase = appDatabase

    @Single
    fun deviceAppDatabase(appDatabase: AppDatabase): DeviceDatabase = appDatabase

    @Single
    fun brewAppDatabase(appDatabase: AppDatabase): BrewDatabase = appDatabase
}
