package app.geeflow.presentation.feature.device.settings.brewing

data class BrewingSettingsViewState(
    val brewBoiler: Boiler = Boiler(),
    val steamBoiler: Boiler = Boiler(),
    val pulseHeatingEnabled: Boolean = false,
    val paddle: Paddle = Paddle(),
    val applyButtonLoading: Boolean = false,
    val applyButtonVisible: Boolean = false,
) {
    data class Boiler(
        val enabled: Boolean = false,
        val actualTemp: Float = 0f,
        val selectedTemp: String = "0",
        val tempList: List<String> = emptyList(),
    )

    data class Paddle(
        val pressure: String = "0.0",
        val pressureList: List<String> = emptyList(),
        val time: String = "0",
        val timeList: List<String> = emptyList(),
    )
}
