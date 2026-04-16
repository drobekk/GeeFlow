package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

data class QuickSettingsViewState(
    val brewBoiler: Boiler = Boiler(),
    val steamBoiler: Boiler = Boiler(),
    val applying: Boolean = false,
) {
    data class Boiler(
        val enabled: Boolean = false,
        val actualTemp: Float = 0f,
        val selectedTemp: String = "0",
        val tempList: List<String> = emptyList(),
    )
}
