package app.geeflow.data.brew.model

import kotlinx.serialization.Serializable

@Serializable
data class SingleDoseSettings(
    val enabled: Boolean = false,
    val grindingSize: Int = DEFAULT_GRINDING_SIZE,
    val grindingSpeed: Int = DEFAULT_GRINDING_SPEED,
) {
    companion object {
        const val MIN_GRINDING_SIZE = 0
        const val MAX_GRINDING_SIZE = 600
        const val MIN_GRINDING_SPEED = 200
        const val MAX_GRINDING_SPEED = 1000
        const val DEFAULT_GRINDING_SIZE = 300
        const val DEFAULT_GRINDING_SPEED = 500
    }
}
