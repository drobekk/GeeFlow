package app.geeflow.data.device

import app.geeflow.data.brew.model.FreeHandControlMode

interface DeviceBrewingControl {
    suspend fun startManualBrewing()
    suspend fun stopManualBrewing()
    suspend fun startFreeVariableBrewing(mode: FreeHandControlMode)
    suspend fun stopFreeVariableBrewing()
    suspend fun setFreeBrewPressureTarget(pressure: Float)
    suspend fun setFreeBrewFlowTarget(flow: Float)
}
