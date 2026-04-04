package dev.drobek.geeflow.presentation.feature.device.settings

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.detailPane
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.listPane
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.destination.DeviceSettings
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsDetailsPlaceholder
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Single
import org.koin.core.parameter.parametersOf

internal typealias Main = DeviceSettings

@Serializable
internal data class BrewingSettings(val deviceId: String) : NavKey

@Serializable
internal data class MaintenanceSettings(val deviceId: String) : NavKey

@Serializable
internal data class ConnectivitySettings(val deviceId: String) : NavKey

@Single
internal class DeviceSettingsNavFeature : NavFeature {

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<Main>(metadata = listPane(detailPlaceholder = { DeviceSettingsDetailsPlaceholder() })) {
            val viewModel = koinViewModel<DeviceSettingsViewModel> { parametersOf(it) }
            DeviceSettingsScreen(viewModel, navigator)
        }

        entry<BrewingSettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<BrewingSettingsViewModel> { parametersOf(it) }
            BrewingSettingsScreen(viewModel, navigator)
        }

        entry<MaintenanceSettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<MaintenanceSettingsViewModel> { parametersOf(it) }
            MaintenanceSettingsScreen(viewModel, navigator)
        }

        entry<ConnectivitySettings>(metadata = detailPane()) {
            val viewModel = koinViewModel<ConnectivitySettingsViewModel> { parametersOf(it) }
            ConnectivitySettingsScreen(viewModel, navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DeviceSettings::class, DeviceSettings.serializer())
            subclass(BrewingSettings::class, BrewingSettings.serializer())
            subclass(MaintenanceSettings::class, MaintenanceSettings.serializer())
            subclass(ConnectivitySettings::class, ConnectivitySettings.serializer())
        }
    }
}
