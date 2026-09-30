package app.geeflow.data.device.model

data class AutoFlushSettings(
    val enabled: Boolean = false,
    val delaySeconds: Int = DefaultDelaySeconds,
) {
    init {
        require(delaySeconds in MinimumDelaySeconds..MaximumDelaySeconds)
    }

    companion object {
        const val DefaultDelaySeconds = 10
        const val MinimumDelaySeconds = 1
        const val MaximumDelaySeconds = 60
    }
}
