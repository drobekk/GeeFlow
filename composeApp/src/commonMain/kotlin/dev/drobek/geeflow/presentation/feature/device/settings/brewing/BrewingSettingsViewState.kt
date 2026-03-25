package dev.drobek.geeflow.presentation.feature.device.settings.brewing

data class BrewingSettingsViewState(
    val brewBoiler: Boiler = Boiler(tempList = (0..110).map { it.toString() }),
    val steamBoiler: Boiler = Boiler(tempList = (0..140).map { it.toString() }),
    val pulseHeatingEnabled: Boolean = false,
    val paddle: Paddle = Paddle()
) {
    data class Boiler(
        val enabled: Boolean = false,
        val actualTemp: Float = 0f,
        val selectedTemp: String = "0",
        val tempList: List<String> = emptyList()
    )

    data class Paddle(
        val pressure: String = "0.0",
        val pressureList: List<String> = (1..120).map { "${it / 10}.${it % 10}" },
        val time: String = "0",
        val timeList: List<String> = (0..60).map { it.toString() },
    )
}
