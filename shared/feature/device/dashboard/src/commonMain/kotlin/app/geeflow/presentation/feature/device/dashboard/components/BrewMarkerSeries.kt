package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.ui.graphics.Color

internal data class BrewMarkerSeries(
    val x: List<Double>,
    val y: List<Double>,
    val color: Color,
    val unit: String,
) {
    init {
        require(x.size == y.size)
    }

    /** Matches the chart's straight segments without inserting entries into the model or history. */
    fun valueAt(time: Double): Double? {
        val index = x.binarySearch(time)
        if (index >= 0) return y[index]
        val next = -index - 1
        if (next == 0 || next == x.size) return null
        val previous = next - 1
        val fraction = (time - x[previous]) / (x[next] - x[previous])
        return y[previous] + fraction * (y[next] - y[previous])
    }
}
