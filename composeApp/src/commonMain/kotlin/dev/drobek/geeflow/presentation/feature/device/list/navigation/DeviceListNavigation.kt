package dev.drobek.geeflow.presentation.feature.device.list.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListScreen
import dev.drobek.geeflow.presentation.feature.device.list.navigation.DeviceListDestinations.DeviceList
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

interface DeviceListNavigation : Navigation {
    fun showAddDevice()
    fun showDeviceDashboard(id: String)
}

sealed interface DeviceListDestinations : NavKey {
    @Serializable
    object DeviceList : DeviceListDestinations
}

val serializerModuleDeviceList = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(DeviceList::class, DeviceList.serializer())
    }
}

fun EntryProviderScope<NavKey>.deviceListEntries(navigation: DeviceListNavigation) {
    entry<DeviceList> {
        DeviceListScreen(navigation)
    }
}
