package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.Cancel
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.ConditionAdded
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.ConditionChanged
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.ConditionRemoved
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.ExperimentClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.InputClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.InputConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.InputDismissed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.NameChanged
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.RampChanged
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.RampDurationChanged
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.RampStartChanged
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.RenameClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.RenameDismissed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.Save
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.TargetChanged
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorEvent.TypeChanged

internal class StepEditorViewModel(
    step: ProfileEditorViewState.Step,
    capabilities: ProfilingCapabilities,
    pressureRange: ClosedFloatingPointRange<Float>,
    flowRange: ClosedFloatingPointRange<Float>,
    stepNumber: Int = 1,
) : BaseViewModel<StepEditorViewState, StepEditorEffect>(
    StepEditorViewState(
        source = step,
        capabilities = capabilities,
        pressureRange = pressureRange,
        flowRange = flowRange,
        stepNumber = stepNumber,
    ),
) {
    private var nextConditionId = viewState.value.conditions.size.toLong()

    fun handleEvent(event: StepEditorEvent) {
        when (event) {
            is InputClicked -> modify { copy(input = event.target) }
            is InputDismissed -> modify { copy(input = null) }
            is InputConfirmed -> confirmInput(value = event.value)
            is RenameClicked -> modify { copy(renaming = true) }
            is RenameDismissed -> modify { copy(renaming = false) }
            is ConditionRemoved -> removeCondition(id = event.id)
            is ConditionAdded -> addCondition()
            is ExperimentClicked -> launch { emitEvent(StepEditorEffect.ExplainExperimental) }
            is Save -> viewState.value.toStep()?.let { launch { emitEvent(StepEditorEffect.Saved(it)) } }
            is Cancel -> launch { emitEvent(StepEditorEffect.Cancelled) }
            else -> updateDraft(event = event)
        }
    }

    private fun addCondition() = modify {
        val metric = BrewMetric.PumpedVolume.takeIf { it in availableConditionMetrics }
            ?: availableConditionMetrics.firstOrNull()
            ?: return@modify this
        val condition = ConditionDraft(
            id = nextConditionId++,
            metric = metric,
            value = when (metric) {
                BrewMetric.PumpedVolume -> "40"
                BrewMetric.CupWeight -> "36"
                BrewMetric.PumpPressure -> pressureRange.start.toString()
                BrewMetric.PumpFlow -> flowRange.start.toString()
                else -> "1"
            },
        )
        copy(conditions = conditions + condition)
    }

    private fun updateDraft(event: StepEditorEvent) = modify {
        when (event) {
            is TypeChanged -> if (controlSupported(event.type)) copy(type = event.type) else this
            is NameChanged -> copy(name = event.value.trim(), renaming = false)
            is TargetChanged -> {
                if (type == StepType.Flow) copy(flow = event.value) else copy(pressure = event.value)
            }

            is RampChanged -> if (rampSupported(event.style)) copy(rampStyle = event.style) else this
            is RampStartChanged -> copy(rampStart = event.start)
            is RampDurationChanged -> copy(rampSeconds = event.value)
            is ConditionChanged -> updateCondition(event)
            else -> this
        }
    }

    private fun StepEditorViewState.updateCondition(event: ConditionChanged): StepEditorViewState = copy(
        conditions = conditions.map { condition ->
            when {
                condition.id != event.condition.id -> condition
                condition.metric == BrewMetric.PhaseTime -> condition.copy(value = event.condition.value)
                event.condition.metric != condition.metric && event.condition.metric !in availableConditionMetrics -> condition
                else -> event.condition
            }
        },
    )

    private fun confirmInput(value: Float) {
        val state = viewState.value
        if (!value.isFinite() || value !in state.inputRange()) return
        when (val input = state.input) {
            StepInput.Target -> handleEvent(TargetChanged(value.toString()))
            StepInput.RampDuration -> handleEvent(RampDurationChanged(value.toString()))
            is StepInput.Condition -> state.conditions.find { it.id == input.id }?.let {
                handleEvent(ConditionChanged(it.copy(value = value.toString())))
            }

            null -> return
        }
        modify { copy(input = null) }
    }

    private fun removeCondition(id: Long) = modify {
        val index = conditions.indexOfFirst { it.id == id }
        if (index < 0 || conditions[index].metric == BrewMetric.PhaseTime) return@modify this
        copy(
            conditions = conditions.filterNot { it.id == id },
        )
    }
}
