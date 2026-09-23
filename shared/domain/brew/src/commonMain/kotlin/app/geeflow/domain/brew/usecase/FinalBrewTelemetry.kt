package app.geeflow.domain.brew.usecase

import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** Status and telemetry may arrive separately. Finalize totals from one fresh frame, without extending the brew. */
internal suspend fun StateFlow<DeviceState>.awaitFinalBrewTelemetry(
    stopped: DeviceState,
    previousTelemetryTime: Instant?,
): DeviceState {
    if (stopped.brewStatus != BrewStatus.Idle || stopped.connectionStatus != ConnectionStatus.Connected) {
        return stopped
    }
    val telemetryTime = stopped.telemetryTime ?: return stopped
    val alreadyFresh = stopped.statusTime?.let { telemetryTime >= it } ?: (telemetryTime != previousTelemetryTime)
    if (alreadyFresh) return stopped
    val finalState = withTimeoutOrNull(FINAL_TELEMETRY_TIMEOUT_MILLIS.milliseconds) {
        first {
            it.brewStatus != BrewStatus.Idle ||
                it.connectionStatus != ConnectionStatus.Connected ||
                (it.telemetryTime?.let { time -> time > telemetryTime } == true)
        }
    }
    return finalState?.takeIf {
        it.brewStatus == BrewStatus.Idle && it.connectionStatus == ConnectionStatus.Connected
    } ?: stopped
}

private const val FINAL_TELEMETRY_TIMEOUT_MILLIS = 1000L
