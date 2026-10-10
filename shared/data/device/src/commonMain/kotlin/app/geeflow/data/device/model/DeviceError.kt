package app.geeflow.data.device.model

/** Single warnNum from holding register 1406; unknown firmware codes must remain visible. */
data class DeviceError(val code: Int) {
    val type: Type? = Type.entries.firstOrNull { it.code == code }
    val isWaterAlarm: Boolean = type == Type.WaterShortage || type == Type.WaterLevelAbnormal

    @Suppress("MagicNumber") // Protocol-defined warnNum values, not calculated quantities.
    enum class Type(val code: Int) {
        WaterShortage(1),
        HeatingTimeout(2),
        WaterReplenishmentTimeout(3),
        ExtractionTimeout(4),
        PressureSensorMissing(5),
        SteamBoilerSensorFailure(6), // NTC2
        BrewBoilerSensorFailure(7), // NTC1
        WaterLevelAbnormal(8),
    }

    companion object {
        fun fromCode(code: Int): DeviceError? = if (code == 0) null else DeviceError(code)
    }
}
