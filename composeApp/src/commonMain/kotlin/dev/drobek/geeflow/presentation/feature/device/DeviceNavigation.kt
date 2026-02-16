package dev.drobek.geeflow.presentation.feature.device

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.AddDevice
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.DeviceList
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceScreen
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

interface DeviceNavigation : Navigation {
    fun showDevicesList()
    fun showAddDevice()
    fun showDeviceDetails(id: String)
}

sealed interface DeviceDestinations : NavKey {
    @Serializable
    object AddDevice : DeviceDestinations

    @Serializable
    object DeviceList : DeviceDestinations
}

fun PolymorphicModuleBuilder<NavKey>.registerDeviceSerializers() {
    subclass(AddDevice::class, AddDevice.serializer())
    subclass(DeviceList::class, DeviceList.serializer())
}

fun EntryProviderScope<NavKey>.deviceEntries(devicesNavigator: DeviceNavigation) {
    entry<AddDevice> {
        AddDeviceScreen(devicesNavigator)
    }
    entry<DeviceList> {
        DeviceListScreen(devicesNavigator)
    }
}
