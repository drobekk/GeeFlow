package app.geeflow.domain.device

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep

enum class LiveModeProbePath(val id: String, val label: String) {
    EncodingMatrix("encoding_matrix", "Legacy target encoding matrix"),
    LiveModeRegister("live_mode_register", "Live mode-register switching"),
    NativeStagePriority("native_stage_priority", "Native per-stage priority"),
}

enum class LiveControl { Pressure, Flow }

enum class LiveModeProbeSequence(val id: String, val label: String) {
    FlowPressureFlowPressure("flow_pressure_flow_pressure", "Flow → Pressure → Flow → Pressure"),
    PressureFlowPressureFlow("pressure_flow_pressure_flow", "Pressure → Flow → Pressure → Flow"),
}

data class LiveModeSwitchProbeStage(
    val index: Int,
    val id: String,
    val control: LiveControl,
    val target: Float,
    val durationMillis: Long,
) {
    val targetUnit get() = if (control == LiveControl.Pressure) "bar" else "ml/s"
}

object LiveModeSwitchProbeScript {
    const val PRESSURE_BAR = 3f
    const val FLOW_ML_PER_SECOND = 2f
    const val STAGE_DURATION_MILLIS = 6_000L
    const val TOTAL_DURATION_MILLIS = 24_000L
    const val MAX_RUN_MILLIS = 32_000L

    val stages = stages(LiveModeProbeSequence.FlowPressureFlowPressure)

    fun stages(sequence: LiveModeProbeSequence): List<LiveModeSwitchProbeStage> {
        val controls = when (sequence) {
            LiveModeProbeSequence.FlowPressureFlowPressure -> listOf(LiveControl.Flow, LiveControl.Pressure, LiveControl.Flow, LiveControl.Pressure)
            LiveModeProbeSequence.PressureFlowPressureFlow -> listOf(LiveControl.Pressure, LiveControl.Flow, LiveControl.Pressure, LiveControl.Flow)
        }
        val counts = mutableMapOf<LiveControl, Int>()
        return controls.mapIndexed { index, control ->
            val occurrence = (counts.getOrDefault(control, 0) + 1).also { counts[control] = it }
            LiveModeSwitchProbeStage(
                index + 1,
                "${control.name.lowercase()}_$occurrence",
                control,
                if (control == LiveControl.Pressure) PRESSURE_BAR else FLOW_ML_PER_SECOND,
                STAGE_DURATION_MILLIS,
            )
        }
    }

    val nativeProfile = nativeProfile(LiveModeProbeSequence.FlowPressureFlowPressure)

    fun nativeProfile(sequence: LiveModeProbeSequence): BrewProfile {
        val selectedStages = stages(sequence)
        return BrewProfile(
        userId = 1L,
        name = "GeeFlow mode-switch diagnostic ${sequence.id}",
        description = "Temporary 4-stage ${sequence.label} mode-switch test. Not saved to the app profile library.",
        finishCondition = Condition.Volume(250f),
        steps = selectedStages.map { stage ->
            when (stage.control) {
                LiveControl.Pressure -> ProfileStep.Pressure(time = (stage.durationMillis / 1_000L).toInt(), pressure = stage.target)
                LiveControl.Flow -> ProfileStep.Flow(time = (stage.durationMillis / 1_000L).toInt(), flow = stage.target)
            }
        },
        )
    }
}
