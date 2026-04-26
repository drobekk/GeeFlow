package app.geeflow

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.core.Logger
import dev.bluefalcon.engine.android.AndroidEngine
import app.geeflow.core.datastore.createAndroidDataStore
import app.geeflow.data.db.AndroidDatabaseDriverFactory
import app.geeflow.data.db.DatabaseDriverFactory
import app.geeflow.data.device.ble.modbus.ModbusPlugin
import app.geeflow.data.device.ble.modbus.installModbus
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import co.touchlab.kermit.Logger as KermitLogger

@Module
@ComponentScan("app.geeflow")
actual class PlatformModule {
    @Single
    fun databaseDriverFactory(context: Context): DatabaseDriverFactory =
        AndroidDatabaseDriverFactory(context)

    @Single
    fun dataStore(context: Context): DataStore<Preferences> = createAndroidDataStore(context)

    @Single
    fun modbusPlugin(): ModbusPlugin = installModbus()

    @Single
    fun blueFalcon(context: Context, modbusPlugin: ModbusPlugin): BlueFalcon = BlueFalcon {
        engine = AndroidEngine(
            context = context.applicationContext as Application,
            logger = KermitBlueFalconLogger,
        )
        install(modbusPlugin)
    }
}

private object KermitBlueFalconLogger : Logger {
    private val log = KermitLogger.withTag("BlueFalcon")
    override fun error(message: String, cause: Throwable?) = log.e(cause) { message }
    override fun warn(message: String, cause: Throwable?) = log.w(cause) { message }
    override fun info(message: String, cause: Throwable?) = log.i(cause) { message }
    override fun debug(message: String, cause: Throwable?) = log.d(cause) { message }
}
