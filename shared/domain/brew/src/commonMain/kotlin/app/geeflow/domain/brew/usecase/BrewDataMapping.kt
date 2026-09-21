package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.device.model.DeviceState

/** Keep the terminal totals without collecting a post-brew tail or accepting counters reset to zero. */
internal fun BrewDataPoint.withFinalTotals(state: DeviceState): BrewDataPoint = copy(
    volume = maxOf(volume, state.volume?.takeIf { it.isFinite() } ?: volume),
    weight = maxOf(weight, state.weight?.takeIf { it.isFinite() } ?: weight),
)
