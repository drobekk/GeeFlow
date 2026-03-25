package dev.drobek.geeflow.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.presentation.feature.device.add.navigation.AddDeviceDestinations.AddDevice
import dev.drobek.geeflow.presentation.feature.device.add.navigation.AddDeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.Clean
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.Dashboard
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardDestinations.QuickSettings
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardNavigation
import dev.drobek.geeflow.presentation.feature.device.list.navigation.DeviceListDestinations.DeviceList
import dev.drobek.geeflow.presentation.feature.device.list.navigation.DeviceListNavigation
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.BrewingSettings
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsDestinations.DeviceSettings
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsNavigation
import dev.drobek.geeflow.presentation.feature.intro.IntroNavigation

class AppNavigation(
    val finish: () -> Unit,
    val backStack: NavBackStack<NavKey>
) : IntroNavigation,
    DeviceDashboardNavigation,
    AddDeviceNavigation,
    DeviceSettingsNavigation,
    DeviceListNavigation {

    override val isAtRoot: Boolean
        get() = backStack.size == 1

    override fun showAddDevice() {
        backStack.add(AddDevice)
    }

    override fun showDeviceDashboard(id: String) {
        backStack.add(Dashboard(id))
    }

    override fun back() {
        if (backStack.size == 1) finish() else backStack.removeLastOrNull()
    }

    override fun clearBackStack() {
        backStack.clear()
    }

    override fun showDevicesList() {
        backStack.add(DeviceList)
    }

    override fun showQuickSettings(deviceId: String) {
        backStack.add(QuickSettings(deviceId))
    }

    override fun showClean(deviceId: String) {
        backStack.add(Clean(deviceId))
    }

    override fun showBrewingSettings(deviceId: String) {
        backStack.add(BrewingSettings(deviceId))
    }

    override fun showDeviceSettings(deviceId: String) {
        backStack.add(DeviceSettings(deviceId))
    }

    override fun backFromSettings() {
        backStack.removeLastOrNull()
        backStack.removeAll { it is DeviceSettingsDestinations }
    }
}
