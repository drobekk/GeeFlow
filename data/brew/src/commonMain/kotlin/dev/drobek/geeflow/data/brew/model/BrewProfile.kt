package dev.drobek.geeflow.data.brew.model

import kotlinx.serialization.SerialName
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
    val position: Int = 0
)

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

@Serializable
enum class ProfileMode(val value: Int) {
    @SerialName("1")
    VariablePressure(1),

    @SerialName("2")
    ConstantPressure(2),

    @SerialName("3")
    FreeVariable(3)
}
