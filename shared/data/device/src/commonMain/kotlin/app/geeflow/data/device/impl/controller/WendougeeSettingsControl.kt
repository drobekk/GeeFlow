@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.DeviceSettingsControl
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.device.model.DeviceState.HeatingMode
import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.withLock

internal class WendougeeSettingsControl(private val owner: WendougeeController) : DeviceSettingsControl {
    private val deviceState get() = owner.mutableState
    private fun requireSession() = owner.requireSession()

    override suspend fun setAgileRing(enabled: Boolean) = owner.operationMutex.withLock {
        val state = deviceState.value
        check(state.agileRingSupported) { "Agile Ring requires LITA firmware 212 or newer" }
        val current = requireNotNull(state.config?.agileRingEnabled) { "Agile Ring state is unavailable" }
        if (current == enabled) return@withLock
        requireSession().modbus.writeMultipleRegisters(WendougeeRegisters.AGILE_RING, listOf(if (enabled) 1 else 0))
        owner.updateState { it.copy(config = it.config?.copy(agileRingEnabled = enabled)) }
    }

    override suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean) {
        val currentConfig = deviceState.value.config
        val isAlreadySet = when (boilerType) {
            BoilerType.Steam -> currentConfig?.steamBoilerEnabled == enabled
            BoilerType.Brew -> currentConfig?.brewBoilerEnabled == enabled
        }
        if (isAlreadySet) return

        val targetRegister = when (boilerType) {
            BoilerType.Steam -> WendougeeRegisters.STEAM_BOILER_STATE
            BoilerType.Brew -> WendougeeRegisters.BREW_BOILER_STATE
        }
        val stateValue = if (enabled) 0x00 else 0x01

        requireSession().modbus.writeSingleRegister(targetRegister, stateValue)

        Logger.withTag(
            TAG,
        ).i { "Boiler ${if (boilerType == BoilerType.Steam) "Steam" else "Brew"} set to $enabled confirmed" }
        owner.updateState { currentState ->
            val config = currentState.config ?: return@updateState currentState
            val newConfig = if (boilerType == BoilerType.Steam) {
                config.copy(steamBoilerEnabled = enabled)
            } else {
                config.copy(brewBoilerEnabled = enabled)
            }
            currentState.copy(config = newConfig)
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        if (temp !in 0..BREW_MAX_TEMP) {
            Logger.withTag(TAG).e { "Brew temperature $temp out of range!" }
            return
        }
        if (deviceState.value.config?.targetBrewTemp?.toInt() == temp) return

        requireSession().modbus.writeSingleRegister(WendougeeRegisters.BREW_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Brew temperature set to $temp°C confirmed" }
        owner.updateState { currentState ->
            val config = currentState.config ?: return@updateState currentState
            currentState.copy(config = config.copy(targetBrewTemp = temp.toFloat()))
        }
    }

    override suspend fun setSteamTemperature(temp: Int) {
        if (temp !in 0..STEAM_MAX_TEMP) {
            Logger.withTag(TAG).e { "Temperature $temp out of safe range!" }
            return
        }
        if (deviceState.value.config?.targetSteamTemp?.toInt() == temp) return

        requireSession().modbus.writeSingleRegister(WendougeeRegisters.STEAM_TEMPERATURE, temp)
        Logger.withTag(TAG).i { "Steam temperature set to $temp°C confirmed" }
        owner.updateState { currentState ->
            val config = currentState.config ?: return@updateState currentState
            currentState.copy(config = config.copy(targetSteamTemp = temp.toFloat()))
        }
    }

    override suspend fun setHeatingMode(heatingMode: HeatingMode) {
        if (deviceState.value.config?.heatingMode == heatingMode) return

        val modeValue = if (heatingMode == HeatingMode.FullSpeed) 0x01 else 0x00
        requireSession().modbus.writeSingleRegister(WendougeeRegisters.HEATING_MODE, modeValue)
        Logger.withTag(TAG).i { "Heating mode set to $heatingMode confirmed" }
        owner.updateState { it.copy(config = it.config?.copy(heatingMode = heatingMode)) }
    }

    override suspend fun setManualBrewPressure(pressure: Float) {
        if (deviceState.value.config?.manualBrewPressure == pressure) return

        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.MANUAL_BREW_PRESSURE,
            listOf((pressure * SENSOR_SCALE_FACTOR).toInt()),
        )
        Logger.withTag(TAG).d { "Manual brew pressure set to $pressure bar confirmed" }
        owner.updateState { state ->
            val config = state.config ?: return@updateState state
            state.copy(config = config.copy(manualBrewPressure = pressure))
        }
    }

    override suspend fun setManualBrewTime(timeSec: Float) {
        if (deviceState.value.config?.manualBrewTimeSec == timeSec) return

        requireSession().modbus.writeMultipleRegisters(
            WendougeeRegisters.MANUAL_BREW_TIME,
            listOf((timeSec * SENSOR_SCALE_FACTOR).toInt()),
        )
        Logger.withTag(TAG).d { "Manual brew time set to $timeSec s confirmed" }
        owner.updateState { state ->
            val config = state.config ?: return@updateState state
            state.copy(config = config.copy(manualBrewTimeSec = timeSec))
        }
    }

    override suspend fun setWaterAlarm(enabled: Boolean) {
        requireSession().modbus.writeMultipleRegisters(WendougeeRegisters.WATER_ALARM, listOf(if (enabled) 1 else 0))
        Logger.withTag(TAG).d { "Water alarm set to $enabled" }
        owner.updateState { state ->
            val config = state.config ?: return@updateState state
            state.copy(config = config.copy(waterAlarmEnabled = enabled))
        }
    }
}
