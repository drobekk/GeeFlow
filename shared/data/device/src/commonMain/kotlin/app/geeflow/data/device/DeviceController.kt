@file:Suppress("TooManyFunctions")

package app.geeflow.data.device

import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.SingleDoseGrinder
import app.geeflow.data.device.model.SmartScale
import kotlinx.coroutines.flow.StateFlow

interface DeviceController {
    val connectionGeneration: Long
    val profilingCapabilities: ProfilingCapabilities
    val deviceState: StateFlow<DeviceState>
    val capabilities: Set<DeviceCapability>
    val constraints: DeviceConstraints
    val foundSingleDoseGrinders: StateFlow<List<SingleDoseGrinder>>
    val foundScales: StateFlow<List<SmartScale>>

    val connection: DeviceConnectionControl
    val settings: DeviceSettingsControl
    val brewing: DeviceBrewingControl
    val maintenance: DeviceMaintenanceControl
    val profiles: DeviceProfileControl
    val accessories: DeviceAccessoryControl
}
