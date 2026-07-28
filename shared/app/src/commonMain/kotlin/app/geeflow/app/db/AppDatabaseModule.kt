package app.geeflow.app.db

import app.cash.sqldelight.EnumColumnAdapter
import app.geeflow.data.brew.impl.ConditionAdapter
import app.geeflow.data.brew.impl.ProfileStepsAdapter
import app.geeflow.data.brew.model.ProfileMode
import appgeeflowdatabrewdb.Brew_profiles
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class AppDatabaseModule {
    @Single
    fun appDatabase(driverFactory: DatabaseDriverFactory): AppDatabase = AppDatabase(
        driver = driverFactory.createDriver(),
        brew_profilesAdapter = Brew_profiles.Adapter(
            modeAdapter = EnumColumnAdapter<ProfileMode>(),
            finishConditionAdapter = ConditionAdapter,
            stepsAdapter = ProfileStepsAdapter,
        ),
    )

    @Single
    fun userAppDatabase(appDatabase: AppDatabase): app.geeflow.data.user.db.AppDatabase = appDatabase

    @Single
    fun deviceAppDatabase(appDatabase: AppDatabase): app.geeflow.data.device.db.AppDatabase = appDatabase

    @Single
    fun brewAppDatabase(appDatabase: AppDatabase): app.geeflow.data.brew.db.AppDatabase = appDatabase
}
