package dev.drobek.geeflow.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.AddDevice
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.DeviceDashboard
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.DeviceList
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations.QuickSettings
import dev.drobek.geeflow.presentation.feature.device.DeviceNavigation
import dev.drobek.geeflow.presentation.feature.intro.IntroNavigation

class AppNavigation(
    val finish: () -> Unit,
    val backStack: NavBackStack<NavKey>
) : IntroNavigation, DeviceNavigation {

    override val isAtRoot: Boolean
        get() = backStack.size == 1

    override fun showAddDevice() {
        backStack.add(AddDevice)
    }

    override fun showDeviceDashboard(id: String) {
        backStack.add(DeviceDashboard(id))
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

    override fun showQuickSettings(id: String) {
        backStack.add(QuickSettings(id))
    }
}
