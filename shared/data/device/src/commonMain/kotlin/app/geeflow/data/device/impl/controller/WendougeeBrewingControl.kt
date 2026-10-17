@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.device.DeviceBrewingControl
import app.geeflow.data.device.model.DeviceState.BrewStatus
import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.withLock

internal class WendougeeBrewingControl(private val owner: WendougeeController) : DeviceBrewingControl {
    private val deviceState get() = owner.mutableState
    private val brewOperationMutex get() = owner.operationMutex
    private fun requireSession() = owner.requireSession()
    private suspend fun refreshBrewStatus(active: WendougeeSession) = owner.refreshBrewStatus(active)
    private suspend fun sendModbusPulse(address: Int, label: String) = owner.sendModbusPulse(address, label)

    override suspend fun startManualBrewing() = brewOperationMutex.withLock {
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Starting manual brew cycle..." }
            sendModbusPulse(COIL_MANUAL_BREW, "Manual Brew")
        }
    }

    override suspend fun stopManualBrewing() = brewOperationMutex.withLock {
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus == BrewStatus.Manual) {
            Logger.withTag(TAG).i { "Stopping manual brew cycle..." }
            sendModbusPulse(COIL_MANUAL_BREW, "Manual Brew Stop")
        }
    }

    override suspend fun startFreeVariableBrewing(mode: FreeHandControlMode) = brewOperationMutex.withLock {
        val isFlow = mode == FreeHandControlMode.Flow
        require(!isFlow || owner.profilingCapabilities.liveFlow != null) {
            "Flow control is not supported by this model"
        }
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus == BrewStatus.Idle) {
            Logger.withTag(TAG).i { "Starting free variable brew (isFlow=$isFlow)..." }
            owner.updateState {
                it.copy(
                    freeHandControlMode = if (isFlow) FreeHandControlMode.Flow else FreeHandControlMode.Pressure,
                    freeHandStopRequested = false,
                )
            }
            requireSession().modbus.writeSingleRegister(WendougeeRegisters.FREE_VAR_PREPARE, FREE_VAR_PREPARE_VALUE)
            requireSession().modbus.writeSingleRegister(WendougeeRegisters.FREE_VAR_MODE, if (isFlow) 1 else 0,)
            sendModbusPulse(COIL_FREE_VAR_BREW, "Free Variable Brew")
            requireSession().modbus.writeMultipleRegisters(WendougeeRegisters.FREE_VAR_TARGET_BASE, listOf(0, 0))
        }
    }

    override suspend fun stopFreeVariableBrewing() = brewOperationMutex.withLock {
        val active = requireSession()
        // The brew command toggles a coil. Refresh status before every attempt to avoid restarting an idle machine.
        refreshBrewStatus(active)
        if (deviceState.value.brewStatus == BrewStatus.FreeVariable) {
            owner.updateState { it.copy(freeHandStopRequested = true) }
            Logger.withTag(TAG).i { "Stopping free variable brew..." }
            active.modbus.writeMultipleRegisters(WendougeeRegisters.FREE_VAR_TARGET_BASE, listOf(0, 0))
            sendModbusPulse(COIL_FREE_VAR_BREW, "Free Variable Brew Stop")
            refreshBrewStatus(active)
        }
    }

    override suspend fun setFreeBrewPressureTarget(pressure: Float) {
        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.FREE_VAR_TARGET_BASE,
            listOf((pressure * SENSOR_SCALE_FACTOR).toInt(), 0),
        )
        Logger.withTag(TAG).d { "Free variable pressure target set to $pressure bar" }
    }

    override suspend fun setFreeBrewFlowTarget(flow: Float) {
        requireNotNull(owner.profilingCapabilities.liveFlow) { "Flow control is not supported by this model" }
        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.FREE_VAR_TARGET_BASE,
            listOf(0, (flow * SENSOR_SCALE_FACTOR).toInt()),
        )
        Logger.withTag(TAG).d { "Free variable flow target set to $flow ml/s" }
    }
}
