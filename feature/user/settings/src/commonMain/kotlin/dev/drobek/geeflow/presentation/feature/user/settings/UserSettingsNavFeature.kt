package dev.drobek.geeflow.presentation.feature.user.settings

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.detailPane
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.listPane
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.destination.UserSettings
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsScreen
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsViewModel
import dev.drobek.geeflow.presentation.feature.user.settings.main.UserSettingsDetailsPlaceholder
import dev.drobek.geeflow.presentation.feature.user.settings.main.UserSettingsScreen
import dev.drobek.geeflow.presentation.feature.user.settings.main.UserSettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Single

@Serializable
internal object AppearanceSettings : NavKey

@Single
internal class UserSettingsNavFeature : NavFeature {

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<UserSettings>(metadata = listPane(detailPlaceholder = { UserSettingsDetailsPlaceholder() })) {
            val viewModel = koinViewModel<UserSettingsViewModel>()
            UserSettingsScreen(viewModel, navigator)
        }

        entry<AppearanceSettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<AppearanceSettingsViewModel>()
            AppearanceSettingsScreen(viewModel, navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(UserSettings::class, UserSettings.serializer())
            subclass(AppearanceSettings::class, AppearanceSettings.serializer())
        }
    }
}
