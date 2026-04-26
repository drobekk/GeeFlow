package app.geeflow.presentation.feature.user.settings

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.detailPane
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.listPane
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.geeflow.navigation.NavFeature
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.destination.UserSettings
import app.geeflow.presentation.feature.user.settings.about.AboutScreen
import app.geeflow.presentation.feature.user.settings.about.AboutViewModel
import app.geeflow.presentation.feature.user.settings.about.licenses.LicensesListScreen
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsScreen
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsViewModel
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesScreen
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesViewModel
import app.geeflow.presentation.feature.user.settings.main.UserSettingsDetailsPlaceholder
import app.geeflow.presentation.feature.user.settings.main.UserSettingsScreen
import app.geeflow.presentation.feature.user.settings.main.UserSettingsViewModel
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsScreen
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Single

@Serializable
internal object ProfileSettings : NavKey

@Serializable
internal object BrewingPreferencesSettings : NavKey

@Serializable
internal object AppearanceSettings : NavKey

@Serializable
internal object AboutSettings : NavKey

@Serializable
internal object LicensesList : NavKey

@Single
internal class UserSettingsNavFeature : NavFeature {

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<UserSettings>(metadata = listPane(detailPlaceholder = { UserSettingsDetailsPlaceholder() })) {
            val viewModel = koinViewModel<UserSettingsViewModel>()
            UserSettingsScreen(viewModel, navigator)
        }

        entry<ProfileSettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<ProfileSettingsViewModel>()
            ProfileSettingsScreen(viewModel, navigator)
        }

        entry<BrewingPreferencesSettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<BrewingPreferencesViewModel>()
            BrewingPreferencesScreen(viewModel, navigator)
        }

        entry<AppearanceSettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<AppearanceSettingsViewModel>()
            AppearanceSettingsScreen(viewModel, navigator)
        }

        entry<AboutSettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<AboutViewModel>()
            AboutScreen(viewModel, navigator)
        }

        entry<LicensesList> {
            LicensesListScreen(navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(UserSettings::class, UserSettings.serializer())
            subclass(ProfileSettings::class, ProfileSettings.serializer())
            subclass(BrewingPreferencesSettings::class, BrewingPreferencesSettings.serializer())
            subclass(AppearanceSettings::class, AppearanceSettings.serializer())
            subclass(AboutSettings::class, AboutSettings.serializer())
            subclass(LicensesList::class, LicensesList.serializer())
        }
    }
}
