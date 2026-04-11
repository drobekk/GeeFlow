package dev.drobek.geeflow.presentation.feature.device.dashboard

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.destination.DeviceDashboard
import dev.drobek.geeflow.platform.permissions.BindEffect
import dev.drobek.geeflow.platform.permissions.PermissionsControllerFactory
import dev.drobek.geeflow.platform.permissions.rememberPermissionsControllerFactory
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceViewModel
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsScreen
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Single
import org.koin.core.parameter.parametersOf

internal typealias Main = DeviceDashboard

@Serializable
internal data class QuickSettings(val deviceId: Long) : NavKey

@Serializable
internal data class QuickMaintenance(val deviceId: Long) : NavKey

@Single
internal class DeviceDashboardNavFeature : NavFeature {
    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<Main> {
            val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
            val viewModel =
                koinViewModel<DeviceDashboardViewModel> { parametersOf(it, factory.createPermissionsController()) }
            val profileListViewModel = koinViewModel<ProfileListViewModel> { parametersOf(it) }
            BindEffect(viewModel.permissionsController)
            DeviceDashboardScreen(viewModel, profileListViewModel, navigator)
        }
        entry<QuickSettings>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
            val viewModel = koinViewModel<QuickSettingsViewModel> { parametersOf(it) }
            QuickSettingsScreen(viewModel, navigator)
        }
        entry<QuickMaintenance>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
            val viewModel = koinViewModel<QuickMaintenanceViewModel> { parametersOf(it) }
            QuickMaintenanceScreen(viewModel, navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DeviceDashboard::class, DeviceDashboard.serializer())
            subclass(QuickSettings::class, QuickSettings.serializer())
            subclass(QuickMaintenance::class, QuickMaintenance.serializer())
        }
    }
}
