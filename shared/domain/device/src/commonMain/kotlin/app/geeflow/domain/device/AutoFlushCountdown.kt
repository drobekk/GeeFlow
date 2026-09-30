package app.geeflow.domain.device

data class AutoFlushCountdown(
    val deviceId: Long,
    val remainingSeconds: Int,
    val totalSeconds: Int,
)
