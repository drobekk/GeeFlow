package dev.drobek.geeflow.presentation.feature.device.settings.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.detailPane
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.listPane
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.BrewingSettings
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.DeviceSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

interface DeviceSettingsNavigation : Navigation {
    fun showBrewingSettings(deviceId: String)
    fun backFromSettings()
}

sealed interface DeviceSettingsDestinations : NavKey {
    @Serializable
    data class DeviceSettings(val deviceId: String) : DeviceSettingsDestinations

    @Serializable
    data class BrewingSettings(val deviceId: String) : DeviceSettingsDestinations
}

val serializerModuleDeviceSettings = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(DeviceSettings::class, DeviceSettings.serializer())
        subclass(BrewingSettings::class, BrewingSettings.serializer())
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
}
