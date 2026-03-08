package dev.drobek.geeflow.presentation.feature.device

import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.platform.permissions.BindEffect
import dev.drobek.geeflow.platform.permissions.PermissionsController
import dev.drobek.geeflow.platform.permissions.PermissionsControllerFactory
import dev.drobek.geeflow.platform.permissions.rememberPermissionsControllerFactory
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.AddDevice
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.DeviceDashboard
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.DeviceList
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.QuickSettings
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceScreen
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewModel
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListScreen
import dev.drobek.geeflow.presentation.feature.device.quicksettings.QuickSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.quicksettings.QuickSettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

interface DeviceNavigation : Navigation {
    fun showDevicesList()
    fun showAddDevice()
    fun showDeviceDashboard(id: String)
    fun showQuickSettings(id: String)
}

sealed interface DeviceDestinations : NavKey {
    @Serializable
    object AddDevice : DeviceDestinations

    @Serializable
    object DeviceList : DeviceDestinations

    @Serializable
    data class DeviceDashboard(val id: String) : DeviceDestinations

    @Serializable
    data class QuickSettings(val id: String) : DeviceDestinations
}

fun PolymorphicModuleBuilder<NavKey>.registerDeviceSerializers() {
    subclass(AddDevice::class, AddDevice.serializer())
    subclass(DeviceList::class, DeviceList.serializer())
    subclass(DeviceDashboard::class, DeviceDashboard.serializer())
    subclass(QuickSettings::class, QuickSettings.serializer())
}

fun EntryProviderScope<NavKey>.deviceEntries(devicesNavigator: DeviceNavigation) {
    entry<AddDevice> {
        val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
        val controller: PermissionsController = remember(factory) { factory.createPermissionsController() }
        val viewModel = koinViewModel<AddDeviceViewModel> { parametersOf(controller) }
        AddDeviceScreen(viewModel, devicesNavigator)
        BindEffect(controller)
    }
    entry<DeviceList> {
        DeviceListScreen(devicesNavigator)
    }
    entry<DeviceDashboard> {
        val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
        val controller: PermissionsController = remember(factory) { factory.createPermissionsController() }
        val viewModel = koinViewModel<DeviceDashboardViewModel> { parametersOf(it, controller) }
        DeviceDashboardScreen(
            viewModel,
            devicesNavigator
        )
        BindEffect(controller)
    }
    entry<QuickSettings>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
        val viewModel = koinViewModel<QuickSettingsViewModel>()
        QuickSettingsScreen(viewModel, devicesNavigator)
    }
}
