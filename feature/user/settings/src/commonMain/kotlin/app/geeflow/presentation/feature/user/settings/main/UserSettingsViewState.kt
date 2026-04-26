package app.geeflow.presentation.feature.user.settings.main

data class UserSettingsViewState(
    val userName: String = "",
    val items: List<Item> = emptyList(),
) {
    sealed interface Item {
        val name: String
        val description: String

        data class Profile(override val name: String, override val description: String) : Item
        data class BrewingPreferences(override val name: String, override val description: String) : Item
        data class AppearanceDisplay(override val name: String, override val description: String) : Item
        data class About(override val name: String, override val description: String) : Item
    }
}
