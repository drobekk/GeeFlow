package dev.drobek.geeflow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.engine.windows.WindowsEngine
import dev.drobek.geeflow.core.datastore.createJvmDataStore
import dev.drobek.geeflow.data.db.DatabaseDriverFactory
import dev.drobek.geeflow.data.db.JvmDatabaseDriverFactory
import dev.drobek.geeflow.data.device.ble.modbus.ModbusPlugin
import dev.drobek.geeflow.data.device.ble.modbus.installModbus
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun databaseDriverFactory(): DatabaseDriverFactory = JvmDatabaseDriverFactory()

    @Single
    fun dataStore(): DataStore<Preferences> = createJvmDataStore()

    @Single
    fun modbusPlugin(): ModbusPlugin = installModbus()

    @Single
    fun blueFalcon(modbusPlugin: ModbusPlugin): BlueFalcon = BlueFalcon {
        engine = WindowsEngine()
        install(modbusPlugin)
    }
}
