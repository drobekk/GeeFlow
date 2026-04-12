package dev.drobek.geeflow.presentation.feature.device.add

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.destination.AddDevice
import dev.drobek.geeflow.platform.permissions.BindEffect
import dev.drobek.geeflow.platform.permissions.PermissionsControllerFactory
import dev.drobek.geeflow.platform.permissions.rememberPermissionsControllerFactory
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
