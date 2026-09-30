package app.geeflow.domain.device

import app.geeflow.data.device.model.DeviceBrewingSettings
import app.geeflow.data.device.model.DeviceState

internal data class AutoFlushMachineState(
    val state: DeviceState,
    val settings: DeviceBrewingSettings,
    val stopping: Boolean,
    val treatManualAsFlush: Boolean,
)
