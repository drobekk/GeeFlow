package dev.drobek.geeflow.data.device.api

import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.MachineState.BoilerType
import dev.drobek.geeflow.domain.device.model.MachineState.HeatingMode
import kotlinx.coroutines.flow.StateFlow

interface DeviceController {
    val machineState: StateFlow<MachineState>

    fun connect(macAddress: String)
    fun disconnect()

    suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean)
    suspend fun setBrewTemperature(temp: Int)
    suspend fun setSteamTemperature(temp: Int)
    suspend fun manualBrewToggle()
    suspend fun triggerShortPress()
    suspend fun setHeatingMode(heatingMode: HeatingMode)
}
