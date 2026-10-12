package app.geeflow.data.device

import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.device.model.DeviceState.HeatingMode

interface DeviceSettingsControl {
    suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean)
    suspend fun setBrewTemperature(temp: Int)
    suspend fun setSteamTemperature(temp: Int)
    suspend fun setHeatingMode(heatingMode: HeatingMode)
    suspend fun setManualBrewPressure(pressure: Float)
    suspend fun setManualBrewTime(timeSec: Float)
    suspend fun setAgileRing(enabled: Boolean)
    suspend fun setWaterAlarm(enabled: Boolean)
}
