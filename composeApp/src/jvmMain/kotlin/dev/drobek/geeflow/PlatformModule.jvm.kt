package dev.drobek.geeflow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.drobek.geeflow.core.datastore.createJvmDataStore
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.JvmDatabaseDriverFactory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun databaseDriverFactory(): DatabaseDriverFactory = JvmDatabaseDriverFactory()

    @Single
    fun dataStore(): DataStore<Preferences> = createJvmDataStore()

    // TODO Compile dll https://github.com/Reedyuk/blue-falcon?tab=readme-ov-file#windows
    @Single
    fun blueFalcon(): BlueFalcon = BlueFalcon(
        log = null,
        context = ApplicationContext(),
        autoDiscoverAllServicesAndCharacteristics = true,
    )
}
