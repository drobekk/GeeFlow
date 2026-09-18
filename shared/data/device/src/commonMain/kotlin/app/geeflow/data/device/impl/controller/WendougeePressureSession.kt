package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.LiveBrewSession
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource

/** Keeps hardware in pressure mode for the entire app-controlled brew. */
internal class WendougeePressureSession(private val controller: DeviceController) : LiveBrewSession {
    override val requiresContinuousUpdates = true
    private val clock = TimeSource.Monotonic.markNow()
    private val pressureRange = requireNotNull(controller.profilingCapabilities.livePressure[PressureLocation.Pump])
    private val regulator = PumpFlowPressureRegulator(maximumPressure = pressureRange.maximum)
    private var lastMeasurement: Instant? = null
    private var lastPressure = 0f
    private var lastRequested: PhaseControl? = null
    private var hasWritten = false

    override suspend fun applyTarget(target: PhaseControl): PhaseControl {
        val pressure = when (target) {
            is PhaseControl.Pressure -> {
                require(target.location == PressureLocation.Pump)
                require(target.bar.isFinite())
                pressureRange.quantize(target.bar)
            }
            is PhaseControl.Flow -> flowPressure(target = target)
            PhaseControl.PumpPause -> 0f
        }
        val quantized = pressureRange.quantize(pressure)
        if (!hasWritten || quantized != lastPressure) {
            controller.setFreeBrewPressureTarget(pressure = quantized)
            lastPressure = quantized
            hasWritten = true
        }
        if (target !is PhaseControl.Flow) {
            regulator.reset(pressure = quantized)
            lastMeasurement = null
        }
        lastRequested = target
        return PhaseControl.Pressure(bar = quantized)
    }

    private fun flowPressure(target: PhaseControl.Flow): Float {
        require(target.millilitresPerSecond.isFinite() && target.millilitresPerSecond >= 0f)
        if (target.millilitresPerSecond == 0f) {
            regulator.reset()
            lastMeasurement = null
            return 0f
        }
        val stamp = requireNotNull(controller.deviceState.value.telemetryTime) { "Missing flow telemetry" }
        val age = (Clock.System.now() - stamp).inWholeMilliseconds
        check(age in 0..TELEMETRY_TIMEOUT_MS) { "Flow telemetry timed out" }
        val telemetry = controller.telemetry()
        val flow = requireNotNull(telemetry[BrewMetric.PumpFlow]) { "Missing pump flow" }
        val measuredPressure = requireNotNull(telemetry[BrewMetric.PumpPressure]) { "Missing pump pressure" }
        check(measuredPressure in 0f..pressureRange.maximum) { "Pump pressure outside supported range" }
        if (lastRequested !is PhaseControl.Flow) {
            regulator.reset(pressure = lastPressure)
            lastMeasurement = null
        }
        if (stamp == lastMeasurement) return lastPressure
        lastMeasurement = stamp
        return regulator.update(
            targetFlow = target.millilitresPerSecond,
            measuredFlow = flow,
            elapsedMillis = clock.elapsedNow().inWholeMilliseconds,
        )
    }

    override suspend fun stop() = controller.stopFreeVariableBrewing()
}

private const val TELEMETRY_TIMEOUT_MS = 2000L
