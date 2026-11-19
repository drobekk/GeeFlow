package app.geeflow.domain.device

import app.geeflow.data.device.BookooScaleReading

enum class WeightCutoffSource { MachineScale, Bookoo }

data class AppWeightCutoff(
    val targetGrams: Float,
    val source: WeightCutoffSource,
    val bookooConnected: () -> Boolean,
    val bookooReading: () -> BookooScaleReading?,
)
