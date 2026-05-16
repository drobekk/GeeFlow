package app.geeflow.presentation.feature.device.add

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.geeflow.navigation.NavFeature
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.destination.AddDevice
import app.geeflow.platform.permissions.BindEffect
import app.geeflow.platform.permissions.PermissionsControllerFactory
import app.geeflow.platform.permissions.rememberPermissionsControllerFactory
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Single
import org.koin.core.parameter.parametersOf

@Single
internal class AddDeviceNavFeature : NavFeature {
    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<AddDevice> {
            val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
            val viewModel = koinViewModel<AddDeviceViewModel> { parametersOf(factory.createPermissionsController()) }
            BindEffect(viewModel.permissionsController)
            AddDeviceScreen(viewModel, navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AddDevice::class, AddDevice.serializer())
        }
    }
}
