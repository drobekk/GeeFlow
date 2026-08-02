package app.geeflow.data.device.model

data class DeviceConstraints(
    val brewTempRange: IntRange,
    val steamTempRange: IntRange,
    val manualBrewPressureRange: IntRange,
    val manualBrewTimeRange: IntRange,
    val cleaningTimeRange: IntRange,
    val cleaningRestRange: IntRange,
    val cleaningCountRange: IntRange,
    val pressureRange: ClosedFloatingPointRange<Float>,
    val flowRange: ClosedFloatingPointRange<Float>,
)
