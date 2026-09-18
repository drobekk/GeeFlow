package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.model.ProfilingCapabilities

internal data class ConditionDraft(
    val id: Long,
    val metric: BrewMetric,
    val comparison: ThresholdComparison = ThresholdComparison.Above,
    val value: String,
)

internal data class StepEditorViewState(
    val source: ProfileEditorViewState.Step,
    val capabilities: ProfilingCapabilities,
    val pressureRange: ClosedFloatingPointRange<Float>,
    val flowRange: ClosedFloatingPointRange<Float>,
    val stepNumber: Int = 1,
    val renaming: Boolean = false,
    val input: StepInput? = null,
    val type: StepType = source.type,
    val name: String = source.phaseName,
    val pressure: String = if (source.type == StepType.Pressure) source.value.toString() else "9",
    val flow: String = if (source.type == StepType.Flow) source.value.toString() else "4",
    val rampStyle: RampStyle = source.ramp.style,
    val rampStart: RampStart = source.ramp.start,
    val rampSeconds: String = if (source.ramp.durationMillis > 0) {
        (source.ramp.durationMillis / MillisecondsPerSecond).toString()
    } else {
        "2"
    },
    val conditions: List<ConditionDraft> = source.editorConditions().mapIndexed { index, condition ->
        ConditionDraft(
            id = index.toLong(),
            metric = condition.metric,
            comparison = condition.comparison,
            value = condition.threshold.toString(),
        )
    },
) {
    val isExperimental get() = rampExperimental || conditions.any { conditionExperimental(it.metric) }
    val target get() = if (type == StepType.Flow) flow else pressure
    val targetRange get() = if (type == StepType.Flow) flowRange else pressureRange
    val rampExperimental get() = type != StepType.Wait && rampStyle !in capabilities.native?.ramps.orEmpty()
    fun conditionExperimental(metric: BrewMetric) = metric !in capabilities.native?.exitMetrics.orEmpty()

    val availableConditionMetrics: List<BrewMetric>
        get() = (capabilities.native?.exitMetrics.orEmpty() + capabilities.telemetry)
            .filter { it != BrewMetric.PhaseTime }
            .sortedBy { it.ordinal }

    fun liveControlSupported(control: StepType): Boolean = when (control) {
        StepType.Pressure -> PressureLocation.Pump in capabilities.livePressure
        StepType.Flow -> capabilities.liveFlow != null
        StepType.Wait -> capabilities.livePause
    }

    fun controlSupported(control: StepType): Boolean = liveControlSupported(control) || when (control) {
        StepType.Pressure -> PressureLocation.Pump in capabilities.native?.pressureLocations.orEmpty()
        StepType.Flow -> capabilities.native?.flow == true
        StepType.Wait -> capabilities.native?.pause == true
    }

    fun rampSupported(style: RampStyle): Boolean = style in capabilities.native?.ramps.orEmpty() ||
        liveControlSupported(type)
}

private const val MillisecondsPerSecond = 1000f

private fun ProfileEditorViewState.Step.editorConditions(): List<ExitCondition> {
    val time = exitConditions.firstOrNull { it.metric == BrewMetric.PhaseTime }
        ?: ExitCondition(BrewMetric.PhaseTime, ThresholdComparison.Above, timeSec.toFloat())
    return listOf(time.copy(comparison = ThresholdComparison.Above)) +
        exitConditions.filterNot { it.metric == BrewMetric.PhaseTime }
}

internal fun StepEditorViewState.inputRange(): ClosedFloatingPointRange<Float> = when (val field = input) {
    StepInput.Target -> targetRange
    StepInput.RampDuration -> MinimumRampSeconds..(conditions.first().value.toFloatOrNull() ?: source.timeSec.toFloat())
    is StepInput.Condition -> when (conditions.find { it.id == field.id }?.metric) {
        BrewMetric.PhaseTime -> 1f..MaximumInputValue
        BrewMetric.PumpPressure -> pressureRange
        BrewMetric.PumpFlow -> flowRange
        else -> 0f..MaximumInputValue
    }
    null -> 0f..0f
}

private const val MinimumRampSeconds = 0.1f
private const val MaximumInputValue = 65535f
