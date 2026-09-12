@file:Suppress("TooManyFunctions")

package app.geeflow.data.device

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.device.model.DeviceState.HeatingMode
import app.geeflow.data.device.model.ProfileIssue
import app.geeflow.data.device.model.ProfileIssueCode
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.SmartScale
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface DeviceController {
    suspend fun stopLiveSession(): Unit = error("Live control is unsupported")
    fun isLiveSessionActive(state: DeviceState): Boolean = false
    val profilingCapabilities: ProfilingCapabilities
        get() = ProfilingCapabilities()
    fun assessNativeProfile(profile: BrewProfile): List<ProfileIssue> =
        listOf(ProfileIssue(ProfileIssueCode.NativeFeature))
    fun telemetry(): BrewTelemetry = BrewTelemetry(emptyMap())
    suspend fun openLiveSession(initial: PhaseControl): LiveBrewSession =
        error("Live profile control is unsupported")
    val deviceState: StateFlow<DeviceState>
    val capabilities: Set<DeviceCapability>
    val constraints: DeviceConstraints
    val foundScales: StateFlow<List<SmartScale>>

    /**
     * Emitted when the controller resolves a different [DeviceConnection] than the one
     * it was asked to connect with (e.g. iOS BlueFalcon returns a new peripheral UUID
     * after a Bluetooth reset). The use case layer persists the updated connection.
     */
    val resolvedConnection: SharedFlow<DeviceConnection>

    fun connect(device: Device)
    fun disconnect()

    suspend fun setBoilerState(boilerType: BoilerType, enabled: Boolean)
    suspend fun setBrewTemperature(temp: Int)
    suspend fun setSteamTemperature(temp: Int)
    suspend fun startManualBrewing()
    suspend fun stopManualBrewing()
    suspend fun startFreeVariableBrewing(isFlow: Boolean)
    suspend fun stopFreeVariableBrewing()
    suspend fun setFreeBrewPressureTarget(pressure: Float)
    suspend fun setFreeBrewFlowTarget(flow: Float)
    suspend fun stopProfileBrewing()
    suspend fun startCleaning()
    suspend fun stopCleaning()
    suspend fun setHeatingMode(heatingMode: HeatingMode)
    suspend fun setManualBrewPressure(pressure: Float)
    suspend fun setManualBrewTime(timeSec: Float)
    suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int)
    suspend fun setWaterAlarm(enabled: Boolean)
    suspend fun startProfileBrewing(profile: BrewProfile)
    suspend fun bindProfile(profile: BrewProfile)

    suspend fun setSmartScaleConnectivity(enabled: Boolean)
    suspend fun requestSmartScaleList()
    suspend fun connectSmartScale(name: String)
    suspend fun disconnectSmartScale()
}
