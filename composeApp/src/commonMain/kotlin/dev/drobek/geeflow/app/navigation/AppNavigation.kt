package dev.drobek.geeflow.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.presentation.feature.device.add.navigation.AddDeviceDestinations
import dev.drobek.geeflow.presentation.feature.device.add.navigation.AddDeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDestinations
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.list.navigation.DeviceListDestinations
import dev.drobek.geeflow.presentation.feature.device.list.navigation.DeviceListNavigation
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsNavigation
import dev.drobek.geeflow.presentation.feature.intro.IntroNavigation

class AppNavigation(
    val finish: () -> Unit,
    val backStack: NavBackStack<NavKey>
) : IntroNavigation,
    DeviceNavigation,
    AddDeviceNavigation,
    DeviceSettingsNavigation,
    DeviceListNavigation {

    override val isAtRoot: Boolean
        get() = backStack.size == 1

    override fun showAddDevice() {
        backStack.add(AddDeviceDestinations.AddDevice)
    }

    override fun showDeviceDashboard(id: String) {
        backStack.add(DeviceDestinations.DeviceDashboard(id))
    }

    override fun back() {
        if (backStack.size == 1) finish() else backStack.removeLastOrNull()
    }

    override fun clearBackStack() {
        backStack.clear()
    }

    override fun showDevicesList() {
        backStack.add(DeviceListDestinations.DeviceList)
    }

    override fun showQuickSettings(id: String) {
        backStack.add(DeviceSettingsDestinations.QuickSettings(id))
    }

    override fun showClean(id: String) {
        backStack.add(DeviceSettingsDestinations.Clean(id))
    }
}
