package app.geeflow.presentation.feature.user.settings.main

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.navigation.destination.UserList
import app.geeflow.navigation.destination.UserSettings
import app.geeflow.navigation.destination.AboutSettings
import app.geeflow.navigation.destination.AppearanceSettings
import app.geeflow.navigation.destination.BrewingPreferencesSettings
import app.geeflow.navigation.destination.ProfileSettings
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ChangeUserClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ItemClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsViewState.Item
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile_description
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class UserSettingsViewModel(
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) : BaseViewModel<UserSettingsViewState, Unit>(UserSettingsViewState()) {

    init {
        buildItems()
        launch {
            getSelectedUserUseCase().collect { user ->
                if (user != null) modify { copy(userName = user.name) }
            }
        }
    }

    fun handleEvent(event: UserSettingsEvent) = when (event) {
        BackClicked -> popTo(UserSettings, inclusive = true)
        ChangeUserClicked -> navigateTo(UserList)
        is ItemClicked -> {
            popTo(UserSettings, false)
            when (event.item) {
                is Item.Profile -> navigateTo(ProfileSettings)
                is Item.BrewingPreferences -> navigateTo(BrewingPreferencesSettings)
                is Item.AppearanceDisplay -> navigateTo(AppearanceSettings)
                is Item.About -> navigateTo(AboutSettings)
            }
        }
    }

    private fun buildItems() = launch {
        val items = listOf(
            Item.AppearanceDisplay(
                name = getString(Res.string.user_settings_appearance),
                description = "",
            ),
            Item.Profile(
                name = getString(Res.string.user_settings_profile),
                description = getString(Res.string.user_settings_profile_description),
            ),
        )
        modify { copy(items = items) }
    }
}
