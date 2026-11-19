package app.geeflow.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object UserSettings : NavKey

@Serializable
@kotlinx.serialization.SerialName("app.geeflow.presentation.feature.user.settings.ProfileSettings")
object ProfileSettings : NavKey

@Serializable
@kotlinx.serialization.SerialName("app.geeflow.presentation.feature.user.settings.BrewingPreferencesSettings")
object BrewingPreferencesSettings : NavKey

@Serializable
@kotlinx.serialization.SerialName("app.geeflow.presentation.feature.user.settings.AppearanceSettings")
object AppearanceSettings : NavKey

@Serializable
@kotlinx.serialization.SerialName("app.geeflow.presentation.feature.user.settings.AboutSettings")
object AboutSettings : NavKey
