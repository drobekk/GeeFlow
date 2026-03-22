package dev.drobek.geeflow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.bluefalcon.ApplicationContext
import dev.bluefalcon.BlueFalcon
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.JvmDatabaseDriverFactory
import dev.drobek.geeflow.data.users.impl.createJvmDataStore
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun databaseDriverFactory(): DatabaseDriverFactory = JvmDatabaseDriverFactory()

    @Single
    fun dataStore(): DataStore<Preferences> = createJvmDataStore()

    @Single
    fun blueFalcon(): BlueFalcon =
        // TODO Compile dll https://github.com/Reedyuk/blue-falcon?tab=readme-ov-file#windows
        BlueFalcon(
            log = null,
            context = ApplicationContext(),
            autoDiscoverAllServicesAndCharacteristics = true
        )
}
