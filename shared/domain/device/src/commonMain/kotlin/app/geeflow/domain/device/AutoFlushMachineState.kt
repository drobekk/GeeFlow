package app.geeflow.domain.device

import app.geeflow.data.device.model.AutoFlushSettings
import app.geeflow.data.device.model.DeviceState

internal data class AutoFlushMachineState(
    val state: DeviceState,
    val settings: AutoFlushSettings,
    val stopping: Boolean,
    val treatManualAsFlush: Boolean,
)
