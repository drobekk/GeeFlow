package dev.drobek.geeflow.data.device.api

import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.device.model.DeviceCapability
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import dev.drobek.geeflow.domain.device.model.MachineState.HeatingMode
import dev.drobek.geeflow.domain.device.model.SmartScale
import kotlinx.coroutines.flow.StateFlow

interface DeviceController {
    val machineState: StateFlow<MachineState>
    val capabilities: Set<DeviceCapability>
    val foundScales: StateFlow<List<SmartScale>>

    fun connect(macAddress: String)
    fun disconnect()

    suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean)
    suspend fun setBrewTemperature(temp: Int)
    suspend fun setSteamTemperature(temp: Int)
    suspend fun startManualBrewing()
    suspend fun stopManualBrewing()
    suspend fun stopProfileBrewing()
    suspend fun startCleaning()
    suspend fun stopCleaning()
    suspend fun setHeatingMode(heatingMode: HeatingMode)
    suspend fun setManualBrewPressure(pressure: Float)
    suspend fun setManualBrewTime(timeSec: Float)
    suspend fun startProfileBrewing(profile: BrewProfile)
    suspend fun bindProfile(profile: BrewProfile)

    suspend fun startSmartScaleSearch()
    suspend fun stopSmartScaleSearch()
    suspend fun connectSmartScale(name: String)
    suspend fun disconnectSmartScale()
}
