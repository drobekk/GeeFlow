package app.geeflow.data.brew.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrewProfile(
    val id: Long = NEW_ID,
    val userId: Long,
    val name: String,
    val description: String,
    val finishCondition: Condition? = null,
    val autoLinkOpen: Boolean = false,
    val program: BrewProgram,
    val position: Int = 0,
) {
    /** Convenience view for simple phase presentation and native adapters. */
    val steps: List<ProfileStep> get() = (program as? BrewProgram.Phases)?.phases.orEmpty().map { phase ->
        val seconds = (phase.plannedDurationMillis() / 1000).toInt()
        when (val control = phase.control) {
            is PhaseControl.Pressure -> ProfileStep.Pressure(seconds, control.bar)
            is PhaseControl.Flow -> ProfileStep.Flow(seconds, control.millilitresPerSecond)
            PhaseControl.PumpPause -> ProfileStep.Wait(seconds)
        }
    }
    val recording: FreeHandRecording? get() = (program as? BrewProgram.Recording)?.recording

    constructor(
        id: Long = NEW_ID,
        userId: Long,
        name: String,
        description: String,
        finishCondition: Condition?,
        steps: List<ProfileStep>,
        position: Int = 0,
        autoLinkOpen: Boolean = false,
    ) : this(id, userId, name, description, finishCondition, autoLinkOpen, steps.toProgram(), position)

    companion object {
        const val NEW_ID = 0L
    }
}

@Serializable
sealed interface ProfileStep {
    val time: Int

    @Serializable
    @SerialName("pressure")
    data class Pressure(override val time: Int, val pressure: Float) : ProfileStep

    @Serializable
    @SerialName("flow")
    data class Flow(override val time: Int, val flow: Float) : ProfileStep

    @Serializable
    @SerialName("wait")
    data class Wait(override val time: Int) : ProfileStep
}

@Serializable
sealed interface Condition {
    @Serializable
    @SerialName("weight")
    data class Weight(val target: Float) : Condition

    @Serializable
    @SerialName("volume")
    data class Volume(val target: Float) : Condition
}
