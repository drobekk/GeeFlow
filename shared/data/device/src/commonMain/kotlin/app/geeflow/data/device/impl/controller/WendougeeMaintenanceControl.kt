@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.DeviceMaintenanceControl
import app.geeflow.data.device.model.DeviceState.BrewStatus
import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.withLock

internal class WendougeeMaintenanceControl(private val owner: WendougeeController) : DeviceMaintenanceControl {
    private val deviceState get() = owner.mutableState
    private val brewOperationMutex get() = owner.operationMutex
    private fun requireSession() = owner.requireSession()
    private suspend fun refreshBrewStatus(active: WendougeeSession) = owner.refreshBrewStatus(active)
    private suspend fun sendModbusPulse(address: Int, label: String) = owner.sendModbusPulse(address, label)

    override suspend fun startCleaning() = brewOperationMutex.withLock {
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Sending start signal for cleaning..." }
            sendModbusPulse(COIL_CLEANING, "Cleaning Procedure")
        }
    }

    override suspend fun stopCleaning() = brewOperationMutex.withLock {
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus == BrewStatus.Cleaning) {
            Logger.withTag(TAG).i { "Sending stop signal for cleaning..." }
            sendModbusPulse(COIL_CLEANING, "Stop Cleaning")
        }
    }

    override suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int) {
        val modbus = requireSession().modbus
        modbus.writeMultipleRegisters(WendougeeRegisters.CLEANING_TIME, listOf((timeSec * SENSOR_SCALE_FACTOR).toInt()))
        modbus.writeMultipleRegisters(
            WendougeeRegisters.CLEANING_STANDBY_TIME,
            listOf((standbySec * SENSOR_SCALE_FACTOR).toInt()),
        )
        modbus.writeMultipleRegisters(WendougeeRegisters.CLEANING_COUNT, listOf(count))
        Logger.withTag(TAG).i { "Cleaning settings: time=${timeSec}s standby=${standbySec}s count=$count" }
        owner.updateState { state ->
            val config = state.config ?: return@updateState state
            state.copy(
                config = config.copy(cleaningTimeSec = timeSec, cleaningStandbySec = standbySec, cleaningCount = count),
            )
        }
    }
}
