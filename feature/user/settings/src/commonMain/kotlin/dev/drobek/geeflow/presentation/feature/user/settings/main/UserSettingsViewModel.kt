package dev.drobek.geeflow.presentation.feature.user.settings.main

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
import dev.drobek.geeflow.navigation.destination.UserList
import dev.drobek.geeflow.navigation.destination.UserSettings
import dev.drobek.geeflow.presentation.feature.user.settings.AboutSettings
import dev.drobek.geeflow.presentation.feature.user.settings.AppearanceSettings
import dev.drobek.geeflow.presentation.feature.user.settings.BrewingPreferencesSettings
import dev.drobek.geeflow.presentation.feature.user.settings.ProfileSettings
import dev.drobek.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ChangeUserClicked
import dev.drobek.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ItemClicked
import dev.drobek.geeflow.presentation.feature.user.settings.main.UserSettingsViewState.Item
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_about
import geeflow.feature.user.settings.generated.resources.user_settings_about_description
import geeflow.feature.user.settings.generated.resources.user_settings_appearance
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_description
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_preferences
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_preferences_description
import geeflow.feature.user.settings.generated.resources.user_settings_profile
import geeflow.feature.user.settings.generated.resources.user_settings_profile_description
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
        is ItemClicked -> when (event.item) {
            is Item.Profile -> navigateTo(ProfileSettings)
            is Item.BrewingPreferences -> navigateTo(BrewingPreferencesSettings)
            is Item.AppearanceDisplay -> navigateTo(AppearanceSettings)
            is Item.About -> navigateTo(AboutSettings)
        }
    }

    private fun buildItems() = launch {
        val items = listOf(
            Item.Profile(
                name = getString(Res.string.user_settings_profile),
                description = getString(Res.string.user_settings_profile_description),
            ),
            Item.BrewingPreferences(
                name = getString(Res.string.user_settings_brewing_preferences),
                description = getString(Res.string.user_settings_brewing_preferences_description),
            ),
            Item.AppearanceDisplay(
                name = getString(Res.string.user_settings_appearance),
                description = getString(Res.string.user_settings_appearance_description),
            ),
            Item.About(
                name = getString(Res.string.user_settings_about),
                description = getString(Res.string.user_settings_about_description),
            ),
        )
        modify { copy(items = items) }
    }
}
