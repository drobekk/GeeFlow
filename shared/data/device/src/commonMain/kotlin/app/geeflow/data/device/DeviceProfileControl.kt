package app.geeflow.data.device

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.device.model.BrewTelemetry
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.ProfileIssue

interface DeviceProfileControl {
    suspend fun stopProfileBrewing()
    suspend fun startProfileBrewing(profile: BrewProfile)
    suspend fun bindProfile(profile: BrewProfile)
    suspend fun openLiveSession(initial: PhaseControl): LiveBrewSession
    suspend fun stopLiveSession()
    fun isLiveSessionActive(state: DeviceState): Boolean
    fun assessNativeProfile(profile: BrewProfile): List<ProfileIssue>
    fun telemetry(): BrewTelemetry
}
