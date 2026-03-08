package dev.drobek.geeflow.presentation.feature.device.settings.quick

data class QuickSettingsViewState(
    val steamBoilerEnabled: Boolean = false,
    val brewBoilerEnabled: Boolean = false,
    val targetSteamTemp: Int = 0,
    val actualSteamTemp: Float = 0f,
    val targetBrewTemp: Int = 0,
    val actualBrewTemp: Float = 0f,
    val selectedSteamTemp: String = "0",
    val selectedBrewTemp: String = "0",
    val steamTempList: List<String> = (0..140).map { it.toString() },
    val brewTempList: List<String> = (0..110).map { it.toString() }
)
