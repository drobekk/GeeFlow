package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseControl

enum class ProfileControlKind {
    Pressure,
    Flow,
    Mixed,
    Recorded,
    Other,
}

internal fun BrewProgram.controlKind(): ProfileControlKind = when (this) {
    is BrewProgram.Recording -> ProfileControlKind.Recorded
    is BrewProgram.Phases -> {
        val controls = phases.map { it.control }.filterNot { it == PhaseControl.PumpPause }.toSet()
        when {
            controls.any { it is PhaseControl.Pressure } && controls.any { it is PhaseControl.Flow } ->
                ProfileControlKind.Mixed

            controls.any { it is PhaseControl.Pressure } -> ProfileControlKind.Pressure
            controls.any { it is PhaseControl.Flow } -> ProfileControlKind.Flow
            else -> ProfileControlKind.Other
        }
    }
}
