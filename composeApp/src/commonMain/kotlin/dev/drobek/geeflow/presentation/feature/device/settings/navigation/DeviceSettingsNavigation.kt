package dev.drobek.geeflow.presentation.feature.device.settings.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.detailPane
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.listPane
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.BrewingSettings
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.ConnectivitySettings
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.DeviceSettings
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.MaintenanceSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

interface DeviceSettingsNavigation : Navigation {
    fun showBrewingSettings(deviceId: String)
    fun showMaintenanceSettings(deviceId: String)
    fun showConnectivitySettings(deviceId: String)
    fun getCurrentDestination(): NavKey?
    fun backFromSettings()
}

sealed interface DeviceSettingsDestinations : NavKey {
    @Serializable
    data class DeviceSettings(val deviceId: String) : DeviceSettingsDestinations

    @Serializable
    data class BrewingSettings(val deviceId: String) : DeviceSettingsDestinations

    @Serializable
    data class MaintenanceSettings(val deviceId: String) : DeviceSettingsDestinations

    @Serializable
    data class ConnectivitySettings(val deviceId: String) : DeviceSettingsDestinations
}

val serializerModuleDeviceSettings = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(DeviceSettings::class, DeviceSettings.serializer())
        subclass(BrewingSettings::class, BrewingSettings.serializer())
        subclass(MaintenanceSettings::class, MaintenanceSettings.serializer())
        subclass(ConnectivitySettings::class, ConnectivitySettings.serializer())
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.deviceSettingsEntries(navigation: DeviceSettingsNavigation) {
    entry<DeviceSettings>(metadata = listPane()) {
        val viewModel = koinViewModel<DeviceSettingsViewModel> { parametersOf(it) }
        DeviceSettingsScreen(viewModel, navigation)
    }

    entry<BrewingSettings>(metadata = detailPane()) {
        val viewModel = koinViewModel<BrewingSettingsViewModel> { parametersOf(it) }
        BrewingSettingsScreen(viewModel, navigation)
    }

    entry<MaintenanceSettings>(metadata = detailPane()) {
        val viewModel = koinViewModel<MaintenanceSettingsViewModel> { parametersOf(it) }
        MaintenanceSettingsScreen(viewModel, navigation)
    }

    entry<ConnectivitySettings>(metadata = detailPane()) {
        val viewModel = koinViewModel<ConnectivitySettingsViewModel> { parametersOf(it) }
        ConnectivitySettingsScreen(viewModel, navigation)
    }
}
