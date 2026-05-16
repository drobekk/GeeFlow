package app.geeflow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.geeflow.core.datastore.createJvmDataStore
import app.geeflow.core.presentation.Platform
import app.geeflow.data.db.DatabaseDriverFactory
import app.geeflow.data.db.JvmDatabaseDriverFactory
import app.geeflow.data.device.ble.modbus.ModbusPlugin
import app.geeflow.data.device.ble.modbus.installModbus
import app.geeflow.platform.JvmPlatform
import dev.bluefalcon.core.BlueFalcon
import dev.bluefalcon.engine.macos.jvm.MacosJvmEngine
import dev.bluefalcon.engine.windows.WindowsEngine
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformModule {
    @Single
    fun platform(): Platform = JvmPlatform()

    @Single
    fun databaseDriverFactory(): DatabaseDriverFactory = JvmDatabaseDriverFactory()

    @Single
    fun dataStore(): DataStore<Preferences> = createJvmDataStore()

    @Single
    fun modbusPlugin(): ModbusPlugin = installModbus()

    @Single
    fun blueFalcon(modbusPlugin: ModbusPlugin): BlueFalcon = BlueFalcon {
        val osName = System.getProperty("os.name")
        engine = if (osName.startsWith("Windows", ignoreCase = true)) {
            WindowsEngine()
        } else {
            MacosJvmEngine()
        }
        install(modbusPlugin)
    }
}
