package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

data class QuickSettingsViewState(
    val brewBoiler: Boiler = Boiler(tempList = (0..110).map { it.toString() }),
    val steamBoiler: Boiler = Boiler(tempList = (0..140).map { it.toString() })
) {
    data class Boiler(
        val enabled: Boolean = false,
        val actualTemp: Float = 0f,
        val selectedTemp: String = "0",
        val tempList: List<String> = emptyList()
    )
}
