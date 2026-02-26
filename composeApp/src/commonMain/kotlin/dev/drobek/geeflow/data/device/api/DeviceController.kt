package dev.drobek.geeflow.data.device.api

import dev.drobek.geeflow.domain.device.model.MachineState
import kotlinx.coroutines.flow.StateFlow

interface DeviceController {
    val machineState: StateFlow<MachineState>

    fun connect(macAddress: String)
    fun disconnect()
    fun triggerManualBrew()
}
