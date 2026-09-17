@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SmartScale
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

    override val constraints: DeviceConstraints = DeviceConstraints(
        brewTempRange = 0..BREW_MAX_TEMP,
        steamTempRange = 0..STEAM_MAX_TEMP,
        manualBrewPressureRange = 1..PADDLE_PRESSURE_MAX_INT,
        manualBrewTimeRange = 0..PADDLE_TIME_MAX,
        cleaningTimeRange = 1..CLEANING_TIME_MAX,
        cleaningRestRange = 1..CLEANING_REST_MAX,
        cleaningCountRange = 1..CLEANING_COUNT_MAX,
        pressureRange = 0f..BREW_PRESSURE_MAX,
        flowRange = 0f..BREW_FLOW_MAX,
    )

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
        DeviceCapability.FlowProfiling,
    )

    override fun connect(device: Device) {
        scope.launch {
            _deviceState.update { it.copy(connectionStatus = DeviceState.ConnectionStatus.Connecting) }
            delay(CONNECT_DELAY_MS)
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
                        waterAlarmEnabled = it.config?.waterAlarmEnabled ?: false,
                    ),
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
                volume = 0f,
            )
        }
        scope.launch {
            val dtSec = BREW_TICK_MS / MS_PER_SECOND
            val durationMs =
                ((_deviceState.value.config?.manualBrewTimeSec ?: DEFAULT_BREW_TIME_SEC) * MS_PER_SECOND).toInt().milliseconds
            val startTime = Clock.System.now()
            var pressure = 0f
            var elapsedMs = 0L
            while (_deviceState.value.brewStatus == DeviceState.BrewStatus.Manual) {
                delay(BREW_TICK_MS)
                elapsedMs += BREW_TICK_MS
                if (Clock.System.now() - startTime >= durationMs) {
                    stopManualBrewing()
                    break
                }
                pressure = (pressure + PRESSURE_RAMP).coerceAtMost(MAX_PRESSURE)
                val flowing = pressure > FLOW_THRESHOLD_PRESSURE
                val flowRate = if (flowing) {
                    ((pressure - FLOW_THRESHOLD_PRESSURE) / PRESSURE_RANGE * MAX_FLOW).coerceIn(0f, MAX_FLOW)
                } else {
                    0f
                }
                val weightActive = elapsedMs >= WEIGHT_ACTIVE_DELAY_MS
                val weightRate = if (weightActive) flowRate * WEIGHT_FLOW_RATIO else 0f
                _deviceState.update { state ->
                    state.copy(
                        pressure = pressure,
                        flowRate = flowRate,
                        weightRate = weightRate,
                        volume = (state.volume ?: 0f) + flowRate * dtSec,
                        weight = (state.weight ?: 0f) + weightRate * dtSec,
                        time = (elapsedMs / BREW_TICK_MS).toInt(),
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
                weightRate = null,
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
                weightRate = null,
            )
        }
    }

    override suspend fun startProfileBrewing(profile: BrewProfile) {
        delay(500.milliseconds)
        _deviceState.update {
            it.copy(brewStatus = DeviceState.BrewStatus.Profile, pressure = 0f, weight = 0f, volume = 0f)
        }
        scope.launch {
            val dtSec = BREW_TICK_MS / MS_PER_SECOND
            var elapsedMs = 0L

            while (_deviceState.value.brewStatus == DeviceState.BrewStatus.Profile) {
                delay(BREW_TICK_MS.milliseconds)
                elapsedMs += BREW_TICK_MS
                val elapsedSec = elapsedMs / MS_PER_SECOND

                val currentStep = findCurrentStep(profile.steps, elapsedSec)
                if (currentStep == null) {
                    stopProfileBrewing()
                    break
                }

                val (targetPressure, targetFlow) = computeTargetValues(currentStep)

                _deviceState.update { state ->
                    val newVolume = (state.volume ?: 0f) + targetFlow * dtSec
                    val weightActive = elapsedMs >= WEIGHT_ACTIVE_DELAY_MS
                    val newWeight = (state.weight ?: 0f) + if (weightActive) targetFlow * WEIGHT_FLOW_RATIO * dtSec else 0f
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
                            time = null,
                        )
                    } else {
                        state.copy(
                            pressure = targetPressure,
                            flowRate = targetFlow,
                            weightRate = if (weightActive) targetFlow * WEIGHT_FLOW_RATIO else 0f,
                            volume = newVolume,
                            weight = newWeight,
                            time = (elapsedMs / BREW_TICK_MS).toInt(),
                        )
                    }
                }
            }
        }
    }

    private fun findCurrentStep(steps: List<ProfileStep>, elapsedSec: Float): ProfileStep? {
        var stepStartSec = 0f
        for (step in steps) {
            val stepEndSec = stepStartSec + step.time
            if (elapsedSec < stepEndSec) return step
            stepStartSec += step.time
        }
        return null
    }

    private fun computeTargetValues(step: ProfileStep): Pair<Float, Float> = when (step) {
        is ProfileStep.Pressure -> {
            val flow = ((step.pressure - FLOW_THRESHOLD_PRESSURE) / PRESSURE_RANGE * MAX_FLOW).coerceIn(0f, MAX_FLOW)
            step.pressure to flow
        }

        is ProfileStep.Flow -> {
            val pressure = (step.flow / MAX_FLOW * PRESSURE_RANGE + FLOW_THRESHOLD_PRESSURE).coerceIn(0f, MAX_PRESSURE)
            pressure to step.flow
        }

        is ProfileStep.Wait -> 0f to 0f
    }

    override suspend fun startCleaning() {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { it.copy(brewStatus = DeviceState.BrewStatus.Cleaning, time = 1) }
        scope.launch {
            val config = _deviceState.value.config ?: return@launch
            val totalSeconds = (config.cleaningTimeSec + config.cleaningStandbySec).toInt() * config.cleaningCount
            while (_deviceState.value.brewStatus == DeviceState.BrewStatus.Cleaning) {
                delay(CLEANING_TICK_MS)
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
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(
                config = when (boilerType) {
                    DeviceState.BoilerType.Steam -> config.copy(steamBoilerEnabled = enabled)
                    DeviceState.BoilerType.Brew -> config.copy(brewBoilerEnabled = enabled)
                },
            )
        }
    }

    override suspend fun setBrewTemperature(temp: Int) {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(targetBrewTemp = temp.toFloat()))
        }
    }

    override suspend fun setSteamTemperature(temp: Int) {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(targetSteamTemp = temp.toFloat()))
        }
    }

    override suspend fun setHeatingMode(heatingMode: DeviceState.HeatingMode) {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(heatingMode = heatingMode))
        }
    }

    override suspend fun setManualBrewPressure(pressure: Float) {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(manualBrewPressure = pressure))
        }
    }

    override suspend fun setManualBrewTime(timeSec: Float) {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(manualBrewTimeSec = timeSec))
        }
    }

    override suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int) {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(
                config = config.copy(cleaningTimeSec = timeSec, cleaningStandbySec = standbySec, cleaningCount = count),
            )
        }
    }

    override suspend fun setWaterAlarm(enabled: Boolean) {
        delay(DEMO_SETTER_DELAY_MS)
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(
                config = config.copy(waterAlarmEnabled = enabled),
                waterLevelAlarm = enabled,
            )
        }
    }

    private var freeVarFlowTarget = DEFAULT_FREE_VAR_FLOW

    override suspend fun startFreeVariableBrewing(isFlow: Boolean) {
        _deviceState.update {
            it.copy(brewStatus = DeviceState.BrewStatus.FreeVariable, pressure = 0f, weight = 0f, volume = 0f)
        }
        scope.launch {
            val dtSec = BREW_TICK_MS / MS_PER_SECOND
            var pressure = 0f
            var flowRate = 0f
            var elapsedMs = 0L
            while (_deviceState.value.brewStatus == DeviceState.BrewStatus.FreeVariable) {
                delay(BREW_TICK_MS)
                elapsedMs += BREW_TICK_MS
                if (isFlow) {
                    val target = freeVarFlowTarget
                    flowRate = approach(flowRate, target, PRESSURE_RAMP)
                    pressure = (flowRate / MAX_FLOW * PRESSURE_RANGE + FLOW_THRESHOLD_PRESSURE).coerceIn(0f, MAX_PRESSURE)
                } else {
                    val target = _deviceState.value.config?.manualBrewPressure ?: DEFAULT_FREE_VAR_PRESSURE
                    pressure = approach(pressure, target, PRESSURE_RAMP)
                    flowRate = if (pressure > FLOW_THRESHOLD_PRESSURE) {
                        ((pressure - FLOW_THRESHOLD_PRESSURE) / PRESSURE_RANGE * MAX_FLOW).coerceIn(0f, MAX_FLOW)
                    } else {
                        0f
                    }
                }
                val weightActive = elapsedMs >= WEIGHT_ACTIVE_DELAY_MS
                val weightRate = if (weightActive) flowRate * WEIGHT_FLOW_RATIO else 0f
                _deviceState.update { state ->
                    state.copy(
                        pressure = pressure,
                        flowRate = flowRate,
                        weightRate = weightRate,
                        volume = (state.volume ?: 0f) + flowRate * dtSec,
                        weight = (state.weight ?: 0f) + weightRate * dtSec,
                        time = (elapsedMs / BREW_TICK_MS).toInt(),
                    )
                }
            }
        }
    }

    override suspend fun stopFreeVariableBrewing() {
        _deviceState.update {
            it.copy(
                brewStatus = DeviceState.BrewStatus.Idle,
                pressure = 0f,
                weight = null,
                volume = null,
                flowRate = null,
                weightRate = null,
            )
        }
    }

    override suspend fun setFreeBrewPressureTarget(pressure: Float) {
        _deviceState.update { state ->
            val config = state.config ?: return
            state.copy(config = config.copy(manualBrewPressure = pressure))
        }
    }

    override suspend fun setFreeBrewFlowTarget(flow: Float) {
        freeVarFlowTarget = flow
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
            delay(SCALE_SEARCH_DELAY_MS)
            if (!_deviceState.value.smartScaleEnabled) return@launch
            _foundScales.value = listOf(
                SmartScale("Bookoo Themis", isConnected = false),
                SmartScale(SLOW_SCALE_NAME, isConnected = false),
            )
            delay(SCALE_SEARCH_DURATION_MS)
            if (_deviceState.value.smartScaleEnabled) {
                _deviceState.update { it.copy(smartScaleSearchActive = false) }
            }
        }
    }

    override suspend fun connectSmartScale(name: String) {
        // The Acaia is deliberately slow to pair so the connection help hint can be exercised.
        delay(if (name == SLOW_SCALE_NAME) SLOW_SCALE_CONNECT_DELAY_MS else SCALE_CONNECT_DELAY_MS)
        val scale = SmartScale(name, isConnected = true)
        _foundScales.update { scales -> scales.map { if (it.name == name) scale else it } }
        _deviceState.update { it.copy(smartScale = scale) }
    }

    override suspend fun disconnectSmartScale() {
        val name = _deviceState.value.smartScale?.name ?: return
        _foundScales.update { scales -> scales.map { if (it.name == name) it.copy(isConnected = false) else it } }
        _deviceState.update { it.copy(smartScale = null) }
    }

    companion object {
        private const val BREW_MAX_TEMP = 110
        private const val STEAM_MAX_TEMP = 140
        private const val PADDLE_PRESSURE_MAX_INT = 120
        private const val PADDLE_TIME_MAX = 60
        private const val BREW_PRESSURE_MAX = 12f
        private const val BREW_FLOW_MAX = 8f
        private const val CLEANING_TIME_MAX = 60
        private const val CLEANING_REST_MAX = 60
        private const val CLEANING_COUNT_MAX = 10

        private const val CONNECT_DELAY_MS = 500L
        private const val BREW_TICK_MS = 100L
        private const val MS_PER_SECOND = 1000f
        private const val DEFAULT_BREW_TIME_SEC = 5f
        private const val PRESSURE_RAMP = 0.3f
        private const val MAX_PRESSURE = 9f
        private const val FLOW_THRESHOLD_PRESSURE = 2f
        private const val PRESSURE_RANGE = 7f
        private const val MAX_FLOW = 6f
        private const val WEIGHT_ACTIVE_DELAY_MS = 5_000L
        private const val WEIGHT_FLOW_RATIO = 0.9f
        private const val DEMO_SETTER_DELAY_MS = 100L
        private const val CLEANING_TICK_MS = 1000L
        private const val SCALE_SEARCH_DELAY_MS = 2000L
        private const val SCALE_SEARCH_DURATION_MS = 8000L
        private const val SCALE_CONNECT_DELAY_MS = 1500L
        private const val SLOW_SCALE_NAME = "Bookoo Themis Ultra"
        private const val SLOW_SCALE_CONNECT_DELAY_MS = 8000L

        private const val DEFAULT_FREE_VAR_PRESSURE = 6f
        private const val DEFAULT_FREE_VAR_FLOW = 6f

        private fun approach(current: Float, target: Float, step: Float): Float = if (current < target) {
            (current + step).coerceAtMost(target)
        } else {
            (current - step).coerceAtLeast(target)
        }
    }
}
