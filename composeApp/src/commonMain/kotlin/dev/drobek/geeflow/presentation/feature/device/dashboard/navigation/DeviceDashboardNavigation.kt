package dev.drobek.geeflow.presentation.feature.device.dashboard.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.platform.permissions.BindEffect
import dev.drobek.geeflow.platform.permissions.PermissionsControllerFactory
import dev.drobek.geeflow.platform.permissions.rememberPermissionsControllerFactory
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.Dashboard
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.QuickMaintenance
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.QuickSettings
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

interface DeviceDashboardNavigation : Navigation {
    fun showDevicesList()
    fun showQuickSettings(deviceId: String)
    fun showQuickMaintenance(deviceId: String)
    fun showDeviceSettings(deviceId: String)
    fun showMaintenanceSettings(deviceId: String)
    fun showConnectivitySettings(deviceId: String)
}

sealed interface DeviceDashboardDestinations : NavKey {
    @Serializable
    data class Dashboard(val deviceId: String) : DeviceDashboardDestinations

    @Serializable
    data class QuickSettings(val deviceId: String) : DeviceDashboardDestinations

    @Serializable
    data class QuickMaintenance(val deviceId: String) : DeviceDashboardDestinations
}

val serializerModuleDeviceDashboard = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(Dashboard::class, Dashboard.serializer())
        subclass(QuickSettings::class, QuickSettings.serializer())
        subclass(QuickMaintenance::class, QuickMaintenance.serializer())
    }
}

fun EntryProviderScope<NavKey>.deviceDashboardEntries(navigation: DeviceDashboardNavigation) {
    entry<Dashboard> {
        val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
        val viewModel = koinViewModel<DeviceDashboardViewModel> { parametersOf(it, factory.createPermissionsController()) }
        val profileListViewModel = koinViewModel<ProfileListViewModel> { parametersOf(it) }
        BindEffect(viewModel.permissionsController)
        DeviceDashboardScreen(viewModel, profileListViewModel, navigation)
    }
    entry<QuickSettings>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
        val viewModel = koinViewModel<QuickSettingsViewModel> { parametersOf(it) }
        QuickSettingsScreen(viewModel, navigation)
    }
    entry<QuickMaintenance>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
        val viewModel = koinViewModel<QuickMaintenanceViewModel> { parametersOf(it) }
        QuickMaintenanceScreen(viewModel, navigation)
    }
}
