@file:Suppress("TooManyFunctions")

package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.device.DeviceProfileControl
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.TimedPumpProfileSupport
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.pumpTelemetry
import app.geeflow.data.device.openFreeHandSession
import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.withLock

internal class WendougeeProfileControl(private val owner: WendougeeController) : DeviceProfileControl {
    private val brewOperationMutex get() = owner.operationMutex
    private val deviceState get() = owner.deviceState
    private fun requireSession() = owner.requireSession()
    private suspend fun refreshBrewStatus(active: WendougeeSession) = owner.refreshBrewStatus(active)
    private suspend fun sendModbusPulse(address: Int, label: String) = owner.sendModbusPulse(address, label)

    override suspend fun stopProfileBrewing() = brewOperationMutex.withLock {
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus == BrewStatus.Profile) {
            Logger.withTag(TAG).i { "Stopping profile brew cycle..." }
            sendModbusPulse(COIL_SHORT_PRESS, "Short Press")
        }
    }

    override suspend fun startProfileBrewing(profile: BrewProfile) = brewOperationMutex.withLock {
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus != BrewStatus.Idle) return@withLock
        Logger.withTag(TAG).i { "Starting profile brew: ${profile.name}" }
        requireSession().modbus.uploadProfile(profile, isBinding = false)
        refreshBrewStatus(requireSession())
        if (deviceState.value.brewStatus != BrewStatus.Idle) return@withLock
        if (profile.recording != null) {
            sendModbusPulse(WendougeeProfileCompiler.profileCommitCoil(isBinding = false), "Commit recorded profile")
        }
        sendModbusPulse(COIL_SHORT_PRESS, "Short Press")
    }

    override suspend fun bindProfile(profile: BrewProfile) = brewOperationMutex.withLock {
        Logger.withTag(TAG).i { "Binding profile to button: ${profile.name}" }

        requireSession().modbus.uploadProfile(profile, isBinding = true)
        if (profile.recording != null) {
            sendModbusPulse(WendougeeProfileCompiler.profileCommitCoil(isBinding = true), "Commit bound recorded profile")
        }

        Logger.withTag(
            TAG,
        ).d { "Profile uploaded, sending bind command to register ${WendougeeRegisters.BIND_PROFILE}" }
        requireSession().modbus.writeMultipleRegisters(WendougeeRegisters.BIND_PROFILE, listOf(1))
    }

    override suspend fun openLiveSession(initial: PhaseControl): LiveBrewSession = owner.openFreeHandSession(initial)

    override suspend fun stopLiveSession() = owner.brewing.stopFreeVariableBrewing()

    override fun isLiveSessionActive(state: DeviceState) = state.brewStatus == BrewStatus.FreeVariable

    override fun assessNativeProfile(profile: BrewProfile) = TimedPumpProfileSupport.assessNative(profile)

    override fun telemetry() = deviceState.value.pumpTelemetry()
}
