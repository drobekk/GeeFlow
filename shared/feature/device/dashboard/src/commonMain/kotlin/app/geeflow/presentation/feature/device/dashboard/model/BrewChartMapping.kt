package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState
import app.geeflow.presentation.feature.device.dashboard.profileeditor.toCondition
import app.geeflow.presentation.feature.device.dashboard.profileeditor.toPhase
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState

internal fun DeviceDashboardViewState.chartModel(profiles: ProfileListViewState): BrewChartModel {
    val selected = profiles.profiles.find { it.selected }
    val history = brew.historyTarget != null
    val program = if (history) brew.phaseProgram else selected?.program
    return BrewChartModel(
        measurements = brew.data,
        targets = brew.historyTarget ?: selected?.targetData.orEmpty(),
        boundaries = if (!history && selected?.experimental == false) {
            program.nativeChartBoundaries(data = brew.data, finishCondition = selected.finishCondition)
        } else {
            program.chartBoundaries(brew.phaseTransitions)
        },
    )
}

internal fun ProfileEditorViewState.chartModel(): BrewChartModel {
    val program = BrewProgram.Phases(steps.map { it.toPhase() })
    return BrewChartModel(
        measurements = brew.data,
        targets = targetData,
        boundaries = if (experimental) {
            program.chartBoundaries(brew.phaseTransitions)
        } else {
            program.nativeChartBoundaries(data = brew.data, finishCondition = finishTarget.toCondition())
        },
    )
}
