package app.geeflow.presentation.feature.device.settings.main

data class DeviceSettingsViewState(
    val deviceName: String = "",
    val items: List<Item> = emptyList(),
) {
    sealed interface Item {
        val name: String
        val description: String

        data class Preference(override val name: String, val destination: androidx.navigation3.runtime.NavKey) : Item {
            override val description: String = ""
        }
        data class Brewing(override val name: String, override val description: String) : Item
        data class Maintenance(override val name: String, override val description: String) : Item
        data class Connectivity(override val name: String, override val description: String) : Item
    }
}
