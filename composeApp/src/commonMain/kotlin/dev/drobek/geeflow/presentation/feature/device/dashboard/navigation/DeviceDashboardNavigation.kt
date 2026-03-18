package dev.drobek.geeflow.presentation.feature.device.dashboard.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.platform.permissions.BindEffect
import dev.drobek.geeflow.platform.permissions.PermissionsControllerFactory
import dev.drobek.geeflow.platform.permissions.rememberPermissionsControllerFactory
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDestinations.DeviceDashboard
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

interface DeviceNavigation : Navigation {
    fun showDevicesList()
    fun showQuickSettings(id: String)
    fun showClean(id: String)
}

sealed interface DeviceDestinations : NavKey {
    @Serializable
    data class DeviceDashboard(val id: String) : DeviceDestinations
}

fun PolymorphicModuleBuilder<NavKey>.registerDeviceSerializers() {
    subclass(DeviceDashboard::class, DeviceDashboard.serializer())
}

fun EntryProviderScope<NavKey>.deviceDashboardEntries(navigation: DeviceNavigation) {
    entry<DeviceDashboard> {
        val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
        val viewModel = koinViewModel<DeviceDashboardViewModel> { parametersOf(it, factory.createPermissionsController()) }
        val profileListViewModel = koinViewModel<ProfileListViewModel>()
        BindEffect(viewModel.permissionsController)
        DeviceDashboardScreen(viewModel, profileListViewModel, navigation)
    }
}
