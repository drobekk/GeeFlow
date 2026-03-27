package dev.drobek.geeflow.data.device.impl.controller

import dev.drobek.geeflow.app.AppCoroutineScope
import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.device.model.DeviceCapability
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.device.model.SmartScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class DemoDeviceController(private val scope: AppCoroutineScope) : DeviceController {

    private val _machineState = MutableStateFlow(MachineState())
    override val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

    private val _foundScales = MutableStateFlow<List<SmartScale>>(emptyList())
    override val foundScales: StateFlow<List<SmartScale>> = _foundScales.asStateFlow()

    override val capabilities: Set<DeviceCapability> = setOf(
        DeviceCapability.SteamBoiler,
        DeviceCapability.BrewBoiler,
        DeviceCapability.HeatingMode,
        DeviceCapability.WaterAlarm,
        DeviceCapability.ManualBrewing,
        DeviceCapability.ProfileBrewing,
        DeviceCapability.CleaningMode,
        DeviceCapability.CleaningSettings,
        DeviceCapability.SmartScaleConnectivity,
        DeviceCapability.SingleDoseGrinderConnectivity,
        DeviceCapability.CommercialGrinderConnectivity,
        DeviceCapability.PressureProfiling,
        DeviceCapability.FlowProfiling
    )

    override fun connect(macAddress: String) {
        scope.launch {
            _machineState.update { it.copy(connectionStatus = MachineState.ConnectionStatus.Connecting) }
            delay(500)
            _machineState.update {
                it.copy(
                    connectionStatus = MachineState.ConnectionStatus.Connected,
                    brewBoilerTemp = 93f,
                    steamBoilerTemp = 125f,
                    config = MachineState.Config(
                        targetBrewTemp = 93f,
                        targetSteamTemp = 125f,
                        brewBoilerEnabled = true,
                        steamBoilerEnabled = true,
                        manualBrewTimeSec = 5f,
                        manualBrewPressure = 9f,
                        cleaningTimeSec = 5f,
                        cleaningStandbySec = 5f,
                        cleaningCount = 3,
                        heatingMode = MachineState.HeatingMode.FullSpeed,
                        waterAlarm = false
                    )
                )
            }
        }
    }

    override fun disconnect() {
        _machineState.update { MachineState() }
    }

    override suspend fun startManualBrewing() {
        _machineState.update { it.copy(brewStatus = MachineState.BrewStatus.Manual, pressure = 0f, weight = 0f, volume = 0f) }
        scope.launch {
            val durationMs = ((_machineState.value.config?.manualBrewTimeSec ?: 5f) * 1000).toInt().milliseconds
            val startTime = Clock.System.now()
            var pressure = 0f
            while (_machineState.value.brewStatus == MachineState.BrewStatus.Manual) {
                delay(100)
                if (Clock.System.now() - startTime >= durationMs) {
                    stopManualBrewing()
                    break
                }
                pressure = (pressure + 0.3f).coerceAtMost(9f)
                val flowing = pressure > 2f
                _machineState.update { state ->
                    state.copy(
                        pressure = pressure,
                        weight = (state.weight ?: 0f) + if (flowing) 0.05f else 0f,
                        volume = (state.volume ?: 0f) + if (flowing) 0.06f else 0f,
                        flowRate = if (flowing) ((pressure - 2f) / 7f * 6f).coerceIn(0f, 6f) else 0f,
                        weightRate = if (flowing) ((pressure - 2f) / 7f * 5f).coerceIn(0f, 5f) else 0f
                    )
                }
            }
        }
    }

    override suspend fun stopManualBrewing() {
        _machineState.update {
            it.copy(
                brewStatus = MachineState.BrewStatus.Idle,
                pressure = 0f,
                weight = null,
                volume = null,
                flowRate = null,
                weightRate = null
            )
        }
    }

    override suspend fun stopProfileBrewing() {
        _machineState.update {
            it.copy(
                brewStatus = MachineState.BrewStatus.Idle,
                pressure = 0f,
                weight = null,
                volume = null,
                flowRate = null,
                weightRate = null
            )
        }
    }

    override suspend fun startProfileBrewing(profile: BrewProfile) {
        _machineState.update { it.copy(brewStatus = MachineState.BrewStatus.Profile, pressure = 0f, weight = 0f, volume = 0f) }
        scope.launch {
            var pressure = 0f
            while (_machineState.value.brewStatus == MachineState.BrewStatus.Profile) {
                delay(100)
                pressure = (pressure + 0.3f).coerceAtMost(9f)
                val flowing = pressure > 2f
                _machineState.update { state ->
                    state.copy(
                        pressure = pressure,
                        weight = (state.weight ?: 0f) + if (flowing) 0.05f else 0f,
                        volume = (state.volume ?: 0f) + if (flowing) 0.06f else 0f,
                        flowRate = if (flowing) ((pressure - 2f) / 7f * 6f).coerceIn(0f, 6f) else 0f,
                        weightRate = if (flowing) ((pressure - 2f) / 7f * 5f).coerceIn(0f, 6f) else 0f
                    )
                }
            }
        }
    }

    override suspend fun startCleaning() {
        _machineState.update { it.copy(brewStatus = MachineState.BrewStatus.Cleaning, time = 1) }
        scope.launch {
            val config = _machineState.value.config ?: return@launch
            val totalSeconds = (config.cleaningTimeSec + config.cleaningStandbySec).toInt() * config.cleaningCount
            while (_machineState.value.brewStatus == MachineState.BrewStatus.Cleaning) {
                delay(1000)
                val current = _machineState.value.time ?: 1
                if (current >= totalSeconds) {
                    _machineState.update { it.copy(brewStatus = MachineState.BrewStatus.Idle, time = null) }
                } else {
                    _machineState.update { it.copy(time = current + 1) }
                }
            }
        }
    }

    override suspend fun stopCleaning() {
        _machineState.update { it.copy(brewStatus = MachineState.BrewStatus.Idle, time = null) }
    }

    override suspend fun setBoilerState(boilerType: MachineState.BoilerType, enabled: Boolean) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(
                config = when (boilerType) {
                    MachineState.BoilerType.Steam -> config.copy(steamBoilerEnabled = enabled)
                    MachineState.BoilerType.Brew -> config.copy(brewBoilerEnabled = enabled)
                }
            )
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(targetBrewTemp = temp.toFloat()))
        }
    }

    override suspend fun setSteamTemperature(temp: Int) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(targetSteamTemp = temp.toFloat()))
        }
    }

    override suspend fun setHeatingMode(heatingMode: MachineState.HeatingMode) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(heatingMode = heatingMode))
        }
    }

    override suspend fun setManualBrewPressure(pressure: Float) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(manualBrewPressure = pressure))
        }
    }

    override suspend fun setManualBrewTime(timeSec: Float) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(manualBrewTimeSec = timeSec))
        }
    }

    override suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(cleaningTimeSec = timeSec, cleaningStandbySec = standbySec, cleaningCount = count))
        }
    }

    override suspend fun setWaterAlarm(enabled: Boolean) {
        _machineState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(waterAlarm = enabled))
        }
    }

    override suspend fun bindProfile(profile: BrewProfile) = Unit
    override suspend fun startSmartScaleSearch() = Unit
    override suspend fun stopSmartScaleSearch() = Unit
    override suspend fun connectSmartScale(name: String) = Unit
    override suspend fun disconnectSmartScale() = Unit
}
