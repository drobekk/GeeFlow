package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.brew.model.plannedDurationMillis
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.LiveBrewSession
import app.geeflow.data.device.ble.modbus.ModbusSession
import app.geeflow.data.device.model.NativeProfilingCapabilities
import app.geeflow.data.device.model.NativeRecordingCapabilities
import app.geeflow.data.device.model.ProfileIssue
import app.geeflow.data.device.model.ProfileIssueCode
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.TargetRange

internal object WendougeeProfiling {
    const val MAX_RECORDING_POINTS = 127
    const val MAX_PRESSURE = 12f
    const val MAX_FLOW = 8f

    private const val RECORDING_INTERVAL_MS = 500L
    private const val MILLISECONDS_PER_SECOND = 1000L
    private const val MAX_PHASE_DURATION_MS = 65535000L

    val capabilities = ProfilingCapabilities(
        native = NativeProfilingCapabilities(
            exitMetrics = setOf(BrewMetric.PhaseTime),
            pressureLocations = setOf(PressureLocation.Pump),
            flow = true,
            pause = true,
            recording = NativeRecordingCapabilities(RECORDING_INTERVAL_MS, MAX_RECORDING_POINTS),
        ),
        livePressure = mapOf(PressureLocation.Pump to TargetRange(0f, MAX_PRESSURE, .1f)),
        liveFlow = TargetRange(0f, MAX_FLOW, .1f),
        livePause = true,
        // Live register changes were acknowledged but did not switch the active regulator in device tests.
        liveModeSwitch = false,
        liveFlowViaPressure = true,
        minimumWriteIntervalMillis = 100L,
        telemetry = setOf(BrewMetric.PumpPressure, BrewMetric.PumpFlow, BrewMetric.PumpedVolume, BrewMetric.CupWeight),
        binding = true,
    )

    private fun BrewPhase.isNativeTiming(): Boolean {
        val nativeExit = exitConditions.isEmpty() || exitConditions.singleOrNull()?.let {
            it.metric == BrewMetric.PhaseTime && it.comparison == ThresholdComparison.Above && it.threshold % 1f == 0f
        } == true
        val noExtensions = nativeExit && minimumDurationMillis == 0L && ramp.style == RampStyle.Instant
        val representable = maximumDurationMillis % MILLISECONDS_PER_SECOND == 0L &&
            maximumDurationMillis in MILLISECONDS_PER_SECOND..MAX_PHASE_DURATION_MS
        return noExtensions && representable && plannedDurationMillis() >= MILLISECONDS_PER_SECOND
    }

    fun assessNative(profile: BrewProfile): List<ProfileIssue> = buildList {
        if (profile.finishCondition == null) add(ProfileIssue(ProfileIssueCode.MissingGlobalGoal))
        when (val program = profile.program) {
            is BrewProgram.Recording -> {
                if (program.recording.playbackPoints().size > MAX_RECORDING_POINTS) {
                    add(ProfileIssue(ProfileIssueCode.RecordingCapacity))
                }
            }

            is BrewProgram.Phases -> {
                // Native steps use whole seconds; waits are attached to a preceding active step.
                program.phases.forEachIndexed { index, phase ->
                    val validControl = when (val control = phase.control) {
                        is PhaseControl.Pressure -> control.location == PressureLocation.Pump && control.bar in 0f..MAX_PRESSURE
                        is PhaseControl.Flow -> control.millilitresPerSecond in 0f..MAX_FLOW
                        is PhaseControl.PumpPause -> index > 0 && program.phases[index - 1].control != PhaseControl.PumpPause
                    }
                    if (!validControl || !phase.isNativeTiming()) {
                        add(ProfileIssue(ProfileIssueCode.NativeFeature, phase.id))
                    }
                }
            }
        }
    }
}

internal suspend fun DeviceController.openFreeHandSession(initial: PhaseControl): LiveBrewSession {
    val flow = initial is PhaseControl.Flow
    startFreeVariableBrewing(flow)

    return object : LiveBrewSession {
        override suspend fun applyTarget(target: PhaseControl): PhaseControl {
            val actual = when (target) {
                is PhaseControl.Flow -> target.copy(
                    millilitresPerSecond = requireNotNull(profilingCapabilities.liveFlow).quantize(target.millilitresPerSecond),
                )

                is PhaseControl.Pressure -> target.copy(
                    bar = requireNotNull(profilingCapabilities.livePressure[target.location]).quantize(target.bar),
                )

                PhaseControl.PumpPause -> target
            }
            when (actual) {
                is PhaseControl.Flow -> {
                    check(flow)
                    setFreeBrewFlowTarget(actual.millilitresPerSecond)
                }

                is PhaseControl.Pressure -> {
                    check(!flow)
                    setFreeBrewPressureTarget(actual.bar)
                }

                PhaseControl.PumpPause -> if (flow) setFreeBrewFlowTarget(0f) else setFreeBrewPressureTarget(0f)
            }
            return actual
        }

        override suspend fun stop() = stopFreeVariableBrewing()
    }
}

internal suspend fun ModbusSession.uploadProfile(profile: BrewProfile, isBinding: Boolean) {
    // Compile and validate the complete program before the first register write.
    val writes = WendougeeProfileCompiler().buildProfileWrites(profile, isBinding)
    for (write in writes) {
        when (write) {
            is ProfileWrite.Single -> writeSingleRegister(write.register, write.value)
            is ProfileWrite.Multiple -> writeMultipleRegisters(write.register, write.values)
        }
    }
}
