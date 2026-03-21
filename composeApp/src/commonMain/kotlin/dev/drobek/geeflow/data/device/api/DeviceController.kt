package dev.drobek.geeflow.data.device.api

import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.device.model.DeviceCapability
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import dev.drobek.geeflow.domain.device.model.MachineState.HeatingMode
import kotlinx.coroutines.flow.StateFlow

interface DeviceController {
    val machineState: StateFlow<MachineState>
    val capabilities: Set<DeviceCapability>

    fun connect(macAddress: String)
    fun disconnect()

    suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean)
    suspend fun setBrewTemperature(temp: Int)
    suspend fun setSteamTemperature(temp: Int)
    suspend fun startManualBrewing()
    suspend fun triggerShortPress()
    suspend fun setHeatingMode(heatingMode: HeatingMode)
    suspend fun stopCleaning()
    suspend fun startCleaning()
    suspend fun stopManualBrewing()
    suspend fun startProfileBrewing(profile: BrewProfile)
    suspend fun bindProfile(profile: BrewProfile)
}
