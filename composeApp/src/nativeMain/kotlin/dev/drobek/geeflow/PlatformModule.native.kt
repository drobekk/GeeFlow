package dev.drobek.geeflow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.bluefalcon.BlueFalcon
import dev.drobek.geeflow.core.datastore.createIosDataStore
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.NativeDatabaseDriverFactory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.UIKit.UIApplication

@Module
actual class PlatformModule {
    @Single
    fun databaseDriverFactory(): DatabaseDriverFactory = NativeDatabaseDriverFactory()

    @Single
    fun dataStore(): DataStore<Preferences> = createIosDataStore()

    @Single
    fun blueFalcon(): BlueFalcon = BlueFalcon(null, UIApplication.sharedApplication)
}
