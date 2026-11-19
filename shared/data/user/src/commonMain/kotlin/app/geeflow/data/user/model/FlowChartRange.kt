package app.geeflow.data.user.model

@Suppress("MagicNumber") // The numeric values are the explicit range presets.
enum class FlowChartRange(val maxMillilitersPerSecond: Int) {
    FOUR(4),
    SIX(6),
    EIGHT(8),
    TWELVE(12),
}
