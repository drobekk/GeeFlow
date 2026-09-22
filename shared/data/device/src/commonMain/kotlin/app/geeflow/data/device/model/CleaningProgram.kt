package app.geeflow.data.device.model

import kotlinx.serialization.Serializable

@Serializable
data class CleaningProgram(val flushSeconds: Int, val restSeconds: Int, val cycles: Int)
