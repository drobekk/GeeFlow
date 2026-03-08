package dev.drobek.geeflow.presentation.feature.device.settings.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.presentation.feature.device.settings.clean.CleanScreen
import dev.drobek.geeflow.presentation.feature.device.settings.clean.CleanViewModel
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.Clean
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.QuickSettings
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.settings.quick.QuickSettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.koin.compose.viewmodel.koinViewModel

interface DeviceSettingsNavigation : Navigation {

}

sealed interface DeviceSettingsDestinations : NavKey {
    @Serializable
    data class QuickSettings(val id: String) : DeviceSettingsDestinations

    @Serializable
    data class Clean(val id: String) : DeviceSettingsDestinations
}

fun PolymorphicModuleBuilder<NavKey>.registerDeviceSettingsSerializers() {
    subclass(QuickSettings::class, QuickSettings.serializer())
    subclass(Clean::class, Clean.serializer())
}

fun EntryProviderScope<NavKey>.deviceSettingsEntries(navigation: DeviceSettingsNavigation) {
    entry<QuickSettings>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
        val viewModel = koinViewModel<QuickSettingsViewModel>()
        QuickSettingsScreen(viewModel, navigation)
    }
    entry<Clean>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
        val viewModel = koinViewModel<CleanViewModel>()
        CleanScreen(viewModel, navigation)
    }
}
