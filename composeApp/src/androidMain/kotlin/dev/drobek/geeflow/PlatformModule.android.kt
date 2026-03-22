package dev.drobek.geeflow

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.Logger
import dev.drobek.geeflow.data.db.AndroidDatabaseDriverFactory
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.users.impl.createAndroidDataStore
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import co.touchlab.kermit.Logger as KermitLogger

@Module
@ComponentScan("dev.drobek.geeflow")
actual class PlatformModule {
    @Single
    fun databaseDriverFactory(context: Context): DatabaseDriverFactory =
        AndroidDatabaseDriverFactory(context)

    @Single
    fun dataStore(context: Context): DataStore<Preferences> =
        createAndroidDataStore(context)

    @Single
    fun blueFalcon(context: Context): BlueFalcon =
        BlueFalcon(
            context = context as Application,
            log = object : Logger {
                override fun error(message: String, cause: Throwable?) {
                    KermitLogger.withTag("BlueFalcon").e(cause) { message }
                }

                override fun warn(message: String, cause: Throwable?) {
                    KermitLogger.withTag("BlueFalcon").w(cause) { message }
                }

                override fun info(message: String, cause: Throwable?) {
                    KermitLogger.withTag("BlueFalcon").i(cause) { message }
                }

                override fun debug(message: String, cause: Throwable?) {
                    KermitLogger.withTag("BlueFalcon").d(cause) { message }
                }
            }
        )
}
