package app.geeflow.data.brew.model

import kotlinx.serialization.Serializable

@Serializable
data class BrewProfile(
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val description: String,
    val mode: ProfileMode = ProfileMode.VariablePressure,
    val finishCondition: Condition,
    val autoLinkOpen: Boolean = false,
    val steps: List<ProfileStep> = emptyList(),
    val position: Int = 0,
)

@Serializable
sealed interface ProfileStep {
    val time: Int

    @Serializable
    data class Pressure(override val time: Int, val pressure: Float) : ProfileStep

    @Serializable
    data class Flow(override val time: Int, val flow: Float) : ProfileStep

    @Serializable
    data class Wait(override val time: Int) : ProfileStep
}

@Serializable
sealed interface Condition {
    @Serializable
    data class Weight(val target: Float) : Condition

    @Serializable
    data class Volume(val target: Float) : Condition
}

@Serializable
enum class ProfileMode {
    VariablePressure,
    ConstantPressure,
    FreeVariable,
}
