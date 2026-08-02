package app.geeflow.presentation.feature.device.dashboard

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import app.geeflow.navigation.NavFeature
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.navigation.destination.FreeControl
import app.geeflow.platform.permissions.BindEffect
import app.geeflow.platform.permissions.PermissionsControllerFactory
import app.geeflow.platform.permissions.rememberPermissionsControllerFactory
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlScreen
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlViewModel
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardScreen
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewModel
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorScreen
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewModel
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceScreen
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceViewModel
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsScreen
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewModel
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

@Serializable
internal data class ProfileEditor(val deviceId: Long, val profileId: Long? = null) : NavKey

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
        entry<FreeControl> {
            val viewModel = koinViewModel<FreeControlViewModel> { parametersOf(it) }
            FreeControlScreen(viewModel, navigator)
        }
        entry<ProfileEditor> {
            val viewModel = koinViewModel<ProfileEditorViewModel> { parametersOf(it) }
            ProfileEditorScreen(viewModel, navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DeviceDashboard::class, DeviceDashboard.serializer())
            subclass(QuickSettings::class, QuickSettings.serializer())
            subclass(QuickMaintenance::class, QuickMaintenance.serializer())
            subclass(FreeControl::class, FreeControl.serializer())
            subclass(ProfileEditor::class, ProfileEditor.serializer())
        }
    }
}
