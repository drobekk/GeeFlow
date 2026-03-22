package dev.drobek.geeflow.presentation.feature.device.dashboard.clean

data class CleanViewState(
    val isCleaning: Boolean = false,
    val flushProgress: Progress = Progress(0, 0),
    val restProgress: Progress = Progress(0, 0),
    val cycleProgress: Progress = Progress(0, 0)
) {
    data class Progress(
        val current: Int,
        val target: Int
    )
}
