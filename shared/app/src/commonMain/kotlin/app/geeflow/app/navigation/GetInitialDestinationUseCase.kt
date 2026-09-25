package app.geeflow.app.navigation

import androidx.navigation3.runtime.NavKey
import app.geeflow.domain.device.usecase.ObserveDevicesUseCase
import app.geeflow.domain.user.usecase.GetUsersUseCase
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.navigation.destination.DeviceList
import app.geeflow.navigation.destination.Intro
import org.koin.core.annotation.Factory

@Factory
class GetInitialDestinationUseCase(
    private val getUsersUseCase: GetUsersUseCase,
    private val observeDevicesUseCase: ObserveDevicesUseCase,
) {
    operator fun invoke(): NavKey {
        val users = getUsersUseCase().value
        val devices = observeDevicesUseCase().value

        return when {
            devices.isNotEmpty() -> {
                val selectedUser = users.find { it.isSelected }
                val favoriteDeviceId = selectedUser?.favoriteDeviceId
                    ?.takeIf { id -> devices.any { it.id == id } }
                val deviceId = favoriteDeviceId ?: devices.first().id
                DeviceDashboard(deviceId)
            }

            users.isNotEmpty() -> DeviceList
            else -> Intro
        }
    }
}
