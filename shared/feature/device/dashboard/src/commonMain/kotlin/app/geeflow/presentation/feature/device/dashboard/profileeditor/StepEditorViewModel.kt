package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.device.model.ProfilingCapabilities

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
    )
) {
    private var nextConditionId = viewState.value.conditions.size.toLong()

    fun handleEvent(event: StepEditorEvent) {
        when (event) {
            is StepEditorEvent.InputClicked -> modify { copy(input = event.target) }
            StepEditorEvent.InputDismissed -> modify { copy(input = null) }
            is StepEditorEvent.InputConfirmed -> confirmInput(value = event.value)
            StepEditorEvent.RenameClicked -> modify { copy(renaming = true) }
            StepEditorEvent.RenameDismissed -> modify { copy(renaming = false) }
            is StepEditorEvent.ConditionRemoved -> removeCondition(id = event.id)
            StepEditorEvent.ConditionAdded -> modify {
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
            StepEditorEvent.ExperimentClicked -> launch { emitEvent(StepEditorEffect.ExplainExperimental) }
            StepEditorEvent.Save -> viewState.value.toStep()?.let { launch { emitEvent(StepEditorEffect.Saved(it)) } }
            StepEditorEvent.Cancel -> launch { emitEvent(StepEditorEffect.Cancelled) }
            else -> updateDraft(event = event)
        }
    }

    private fun updateDraft(event: StepEditorEvent) = modify {
        when (event) {
            is StepEditorEvent.TypeChanged -> if (controlSupported(event.type)) copy(type = event.type) else this
            is StepEditorEvent.NameChanged -> copy(name = event.value.trim(), renaming = false)
            is StepEditorEvent.TargetChanged -> {
                if (type == StepType.Flow) copy(flow = event.value) else copy(pressure = event.value)
            }
            is StepEditorEvent.RampChanged -> if (rampSupported(event.style)) copy(rampStyle = event.style) else this
            is StepEditorEvent.RampStartChanged -> copy(rampStart = event.start)
            is StepEditorEvent.RampDurationChanged -> copy(rampSeconds = event.value)
            is StepEditorEvent.ConditionChanged -> copy(
                conditions = conditions.map { condition ->
                    when {
                        condition.id != event.condition.id -> condition
                        condition.metric == BrewMetric.PhaseTime -> condition.copy(value = event.condition.value)
                        event.condition.metric != condition.metric &&
                            event.condition.metric !in availableConditionMetrics -> condition
                        else -> event.condition
                    }
                },
            )
            else -> this
        }
    }

    private fun confirmInput(value: Float) {
        val state = viewState.value
        if (!value.isFinite() || value !in state.inputRange()) return
        when (val input = state.input) {
            StepInput.Target -> handleEvent(StepEditorEvent.TargetChanged(value.toString()))
            StepInput.RampDuration -> handleEvent(StepEditorEvent.RampDurationChanged(value.toString()))
            is StepInput.Condition -> state.conditions.find { it.id == input.id }?.let {
                handleEvent(StepEditorEvent.ConditionChanged(it.copy(value = value.toString())))
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
