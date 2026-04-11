package dev.drobek.geeflow.data.device.impl.controller

import dev.drobek.geeflow.data.brew.model.BrewProfile
import dev.drobek.geeflow.data.brew.model.Condition
import dev.drobek.geeflow.data.brew.model.ProfileStep
import dev.drobek.geeflow.data.device.DeviceController
import dev.drobek.geeflow.data.device.model.Device
import dev.drobek.geeflow.data.device.model.DeviceCapability
import dev.drobek.geeflow.data.device.model.DeviceConnection
import dev.drobek.geeflow.data.device.model.DeviceState
import dev.drobek.geeflow.data.device.model.SmartScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class DemoDeviceController(private val scope: CoroutineScope) : DeviceController {

    private val _deviceState = MutableStateFlow(DeviceState())
    override val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _foundScales = MutableStateFlow<List<SmartScale>>(emptyList())
    override val foundScales: StateFlow<List<SmartScale>> = _foundScales.asStateFlow()

    private val _resolvedConnection = MutableSharedFlow<DeviceConnection>(extraBufferCapacity = 1)
    override val resolvedConnection: SharedFlow<DeviceConnection> = _resolvedConnection.asSharedFlow()

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

    override fun connect(device: Device) {
        scope.launch {
            _deviceState.update { it.copy(connectionStatus = DeviceState.ConnectionStatus.Connecting) }
            delay(500)
            _deviceState.update {
                it.copy(
                    connectionStatus = DeviceState.ConnectionStatus.Connected,
                    brewBoilerTemp = 93f,
                    steamBoilerTemp = 125f,
                    pressure = 0.0f,
                    smartScale = it.smartScale,
                    smartScaleEnabled = it.smartScaleEnabled,
                    smartScaleSearchActive = it.smartScaleSearchActive,
                    waterLevelAlarm = it.waterLevelAlarm,
                    config = DeviceState.Config(
                        targetBrewTemp = it.config?.targetBrewTemp ?: 93f,
                        targetSteamTemp = it.config?.targetSteamTemp ?: 125f,
                        brewBoilerEnabled = it.config?.brewBoilerEnabled ?: true,
                        steamBoilerEnabled = it.config?.steamBoilerEnabled ?: true,
                        manualBrewTimeSec = it.config?.manualBrewTimeSec ?: 5f,
                        manualBrewPressure = it.config?.manualBrewPressure ?: 9f,
                        cleaningTimeSec = it.config?.cleaningTimeSec ?: 5f,
                        cleaningStandbySec = it.config?.cleaningStandbySec ?: 5f,
                        cleaningCount = it.config?.cleaningCount ?: 3,
                        heatingMode = it.config?.heatingMode ?: DeviceState.HeatingMode.FullSpeed,
                        waterAlarmEnabled = it.config?.waterAlarmEnabled ?: false
                    )
                )
            }
        }
    }

    override fun disconnect() {
        _deviceState.update { DeviceState() }
    }

    override suspend fun startManualBrewing() {
        _deviceState.update {
            it.copy(
                brewStatus = DeviceState.BrewStatus.Manual,
                pressure = 0f,
                weight = 0f,
                volume = 0f
            )
        }
        scope.launch {
            val tickMs = 100L
            val dtSec = tickMs / 1000f
            val durationMs = ((_deviceState.value.config?.manualBrewTimeSec ?: 5f) * 1000).toInt().milliseconds
            val startTime = Clock.System.now()
            var pressure = 0f
            var elapsedMs = 0L
            while (_deviceState.value.brewStatus == DeviceState.BrewStatus.Manual) {
                delay(tickMs)
                elapsedMs += tickMs
                if (Clock.System.now() - startTime >= durationMs) {
                    stopManualBrewing()
                    break
                }
                pressure = (pressure + 0.3f).coerceAtMost(9f)
                val flowing = pressure > 2f
                val flowRate = if (flowing) ((pressure - 2f) / 7f * 6f).coerceIn(0f, 6f) else 0f
                val weightActive = elapsedMs >= 5_000L
                val weightRate = if (weightActive) flowRate * 0.9f else 0f
                _deviceState.update { state ->
                    state.copy(
                        pressure = pressure,
                        flowRate = flowRate,
                        weightRate = weightRate,
                        volume = (state.volume ?: 0f) + flowRate * dtSec,
                        weight = (state.weight ?: 0f) + weightRate * dtSec,
                        time = (elapsedMs / 100).toInt()
                    )
                }
            }
        }
    }

    override suspend fun stopManualBrewing() {
        _deviceState.update {
            it.copy(
                brewStatus = DeviceState.BrewStatus.Idle,
                pressure = 0f,
                weight = null,
                volume = null,
                flowRate = null,
                weightRate = null
            )
        }
    }

    override suspend fun stopProfileBrewing() {
        _deviceState.update {
            it.copy(
                brewStatus = DeviceState.BrewStatus.Idle,
                pressure = 0f,
                weight = null,
                volume = null,
                flowRate = null,
                weightRate = null
            )
        }
    }

    override suspend fun startProfileBrewing(profile: BrewProfile) {
        _deviceState.update {
            it.copy(
                brewStatus = DeviceState.BrewStatus.Profile,
                pressure = 0f,
                weight = 0f,
                volume = 0f
            )
        }
        scope.launch {
            val tickMs = 100L
            val dtSec = tickMs / 1000f
            var elapsedMs = 0L

            while (_deviceState.value.brewStatus == DeviceState.BrewStatus.Profile) {
                delay(tickMs)
                elapsedMs += tickMs
                val elapsedSec = elapsedMs / 1000f

                // Find current step based on elapsed time
                var stepStartSec = 0f
                var currentStep: ProfileStep? = null
                for (step in profile.steps) {
                    val stepEndSec = stepStartSec + step.time
                    if (elapsedSec < stepEndSec) {
                        currentStep = step
                        break
                    }
                    stepStartSec += step.time
                }

                // All steps completed
                if (currentStep == null) {
                    stopProfileBrewing()
                    break
                }

                val targetPressure: Float
                val targetFlow: Float
                when (currentStep) {
                    is ProfileStep.Pressure -> {
                        targetPressure = currentStep.pressure
                        targetFlow = ((currentStep.pressure - 2f) / 7f * 6f).coerceIn(0f, 6f)
                    }

                    is ProfileStep.Flow -> {
                        targetFlow = currentStep.flow
                        targetPressure = (currentStep.flow / 6f * 7f + 2f).coerceIn(0f, 9f)
                    }

                    is ProfileStep.Wait -> {
                        targetPressure = 0f
                        targetFlow = 0f
                    }
                }

                _deviceState.update { state ->
                    val newVolume = (state.volume ?: 0f) + targetFlow * dtSec
                    val weightActive = elapsedMs >= 5_000L
                    val newWeight = (state.weight ?: 0f) + if (weightActive) targetFlow * 0.9f * dtSec else 0f

                    val finished = when (val cond = profile.finishCondition) {
                        is Condition.Weight -> newWeight >= cond.target
                        is Condition.Volume -> newVolume >= cond.target
                    }

                    if (finished) {
                        state.copy(
                            brewStatus = DeviceState.BrewStatus.Idle,
                            pressure = 0f,
                            weight = null,
                            volume = null,
                            flowRate = null,
                            weightRate = null,
                            time = null
                        )
                    } else {
                        state.copy(
                            pressure = targetPressure,
                            flowRate = targetFlow,
                            weightRate = if (weightActive) targetFlow * 0.9f else 0f,
                            volume = newVolume,
                            weight = newWeight,
                            time = (elapsedMs / 100).toInt()
                        )
                    }
                }
            }
        }
    }

    override suspend fun startCleaning() {
        delay(100)
        _deviceState.update { it.copy(brewStatus = DeviceState.BrewStatus.Cleaning, time = 1) }
        scope.launch {
            val config = _deviceState.value.config ?: return@launch
            val totalSeconds = (config.cleaningTimeSec + config.cleaningStandbySec).toInt() * config.cleaningCount
            while (_deviceState.value.brewStatus == DeviceState.BrewStatus.Cleaning) {
                delay(1000)
                val current = _deviceState.value.time ?: 1
                if (current >= totalSeconds) {
                    _deviceState.update { it.copy(brewStatus = DeviceState.BrewStatus.Idle, time = null) }
                } else {
                    _deviceState.update { it.copy(time = current + 1) }
                }
            }
        }
    }

    override suspend fun stopCleaning() {
        _deviceState.update { it.copy(brewStatus = DeviceState.BrewStatus.Idle, time = null) }
    }

    override suspend fun setBoilerState(boilerType: DeviceState.BoilerType, enabled: Boolean) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(
                config = when (boilerType) {
                    DeviceState.BoilerType.Steam -> config.copy(steamBoilerEnabled = enabled)
                    DeviceState.BoilerType.Brew -> config.copy(brewBoilerEnabled = enabled)
                }
            )
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(targetBrewTemp = temp.toFloat()))
        }
    }

    override suspend fun setSteamTemperature(temp: Int) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(targetSteamTemp = temp.toFloat()))
        }
    }

    override suspend fun setHeatingMode(heatingMode: DeviceState.HeatingMode) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(heatingMode = heatingMode))
        }
    }

    override suspend fun setManualBrewPressure(pressure: Float) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(manualBrewPressure = pressure))
        }
    }

    override suspend fun setManualBrewTime(timeSec: Float) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(manualBrewTimeSec = timeSec))
        }
    }

    override suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(
                config = config.copy(cleaningTimeSec = timeSec, cleaningStandbySec = standbySec, cleaningCount = count)
            )
        }
    }

    override suspend fun setWaterAlarm(enabled: Boolean) {
        delay(100)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(
                config = config.copy(waterAlarmEnabled = enabled),
                waterLevelAlarm = enabled
            )
        }
    }

    override suspend fun bindProfile(profile: BrewProfile) = Unit

    override suspend fun setSmartScaleConnectivity(enabled: Boolean) {
        _deviceState.update { it.copy(smartScaleEnabled = enabled) }
        if (enabled) {
            _foundScales.value = emptyList()
            simulateScaleSearch()
        } else {
            _deviceState.update { it.copy(smartScaleSearchActive = false, smartScale = null) }
            _foundScales.value = emptyList()
        }
    }

    override suspend fun requestSmartScaleList() {
        if (!_deviceState.value.smartScaleEnabled) return
        simulateScaleSearch()
    }

    private fun simulateScaleSearch() {
        scope.launch {
            _deviceState.update { it.copy(smartScaleSearchActive = true) }
            delay(2000)
            if (!_deviceState.value.smartScaleEnabled) return@launch
            _foundScales.value = listOf(
                SmartScale("Bookoo Themis Ultra", isConnected = false),
                SmartScale("Acaia Lunar", isConnected = false)
            )
            delay(8000)
            if (_deviceState.value.smartScaleEnabled) {
                _deviceState.update { it.copy(smartScaleSearchActive = false) }
            }
        }
    }

    override suspend fun connectSmartScale(name: String) {
        delay(1500)
        val scale = SmartScale(name, isConnected = true)
        _foundScales.update { scales -> scales.map { if (it.name == name) scale else it } }
        _deviceState.update { it.copy(smartScale = scale) }
    }

    override suspend fun disconnectSmartScale() {
        val name = _deviceState.value.smartScale?.name ?: return
        _foundScales.update { scales -> scales.map { if (it.name == name) it.copy(isConnected = false) else it } }
        _deviceState.update { it.copy(smartScale = null) }
    }
}
