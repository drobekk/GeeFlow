package dev.drobek.geeflow.presentation.feature.device.add.navigation

import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.platform.permissions.BindEffect
import dev.drobek.geeflow.platform.permissions.PermissionsController
import dev.drobek.geeflow.platform.permissions.PermissionsControllerFactory
import dev.drobek.geeflow.platform.permissions.rememberPermissionsControllerFactory
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceScreen
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewModel
import dev.drobek.geeflow.presentation.feature.device.add.navigation.AddDeviceDestinations.AddDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

interface AddDeviceNavigation : Navigation {
    fun showDevicesList()
}

sealed interface AddDeviceDestinations : NavKey {
    @Serializable
    object AddDevice : AddDeviceDestinations
}

fun PolymorphicModuleBuilder<NavKey>.registerAddDeviceSerializers() {
    subclass(AddDevice::class, AddDevice.serializer())
}

fun EntryProviderScope<NavKey>.addDeviceEntries(navigation: AddDeviceNavigation) {
    entry<AddDevice> {
        val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
        val controller: PermissionsController = remember(factory) { factory.createPermissionsController() }
        val viewModel = koinViewModel<AddDeviceViewModel> { parametersOf(controller) }
        AddDeviceScreen(viewModel, navigation)
        BindEffect(controller)
    }
}
