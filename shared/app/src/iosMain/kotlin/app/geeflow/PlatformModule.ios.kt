package app.geeflow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.geeflow.app.db.DatabaseDriverFactory
import app.geeflow.app.db.NativeDatabaseDriverFactory
import app.geeflow.core.datastore.createIosDataStore
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.engine.ios.IosEngine
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun databaseDriverFactory(): DatabaseDriverFactory = NativeDatabaseDriverFactory()

    @Single
    fun dataStore(): DataStore<Preferences> = createIosDataStore()

    @Single
    fun blueFalcon(): BlueFalcon = BlueFalcon {
        engine = IosEngine()
    }
}
