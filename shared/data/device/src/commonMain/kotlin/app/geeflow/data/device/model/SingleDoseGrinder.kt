package app.geeflow.data.device.model

data class SingleDoseGrinder(
    val name: String,
    val isConnected: Boolean,
    /** FF55 header returned with the grinder's command 20 status. */
    val standing: Int? = null,
    val operationStatus: Int? = null,
)
