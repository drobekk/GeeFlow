package app.geeflow.data.device.model

data class DeviceBrewingSettings(
    val treatManualAsFlush: Boolean = true,
    val autoFlushEnabled: Boolean = false,
    val autoFlushDelaySeconds: Int = DefaultDelaySeconds,
) {
    init {
        require(autoFlushDelaySeconds in MinimumDelaySeconds..MaximumDelaySeconds)
    }

    companion object {
        const val DefaultDelaySeconds = 10
        const val MinimumDelaySeconds = 1
        const val MaximumDelaySeconds = 60
    }
}
