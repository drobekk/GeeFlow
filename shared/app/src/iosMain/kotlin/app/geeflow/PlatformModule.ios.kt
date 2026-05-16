package app.geeflow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.geeflow.core.datastore.createIosDataStore
import app.geeflow.data.db.DatabaseDriverFactory
import app.geeflow.data.db.NativeDatabaseDriverFactory
import app.geeflow.data.device.ble.modbus.ModbusPlugin
import app.geeflow.data.device.ble.modbus.installModbus
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
    fun modbusPlugin(): ModbusPlugin = installModbus()

    @Single
    fun blueFalcon(modbusPlugin: ModbusPlugin): BlueFalcon = BlueFalcon {
        engine = IosEngine()
        install(modbusPlugin)
    }
}
