package dev.drobek.geeflow.app.navigation

import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.domain.device.usecase.GetDevicesUseCase
import dev.drobek.geeflow.domain.user.usecase.GetUsersUseCase
import dev.drobek.geeflow.presentation.feature.device.DeviceDestinations
import dev.drobek.geeflow.presentation.feature.intro.IntroDestinations
import org.koin.core.annotation.Factory

@Factory
class GetInitialDestinationUseCase(
    private val getUsersUseCase: GetUsersUseCase,
    private val getDevicesUseCase: GetDevicesUseCase
) {
    operator fun invoke(): NavKey {
        val users = getUsersUseCase().value
        val devices = getDevicesUseCase().value

        return when {
            devices.isNotEmpty() -> {
                val selectedUser = users.find { it.isSelected }
                val deviceId = selectedUser?.favoriteDeviceSerialNumber ?: devices.first().serialNumber
                DeviceDestinations.DeviceDashboard(deviceId)
            }

            users.isNotEmpty() -> DeviceDestinations.DeviceList
            else -> IntroDestinations.CreateUser
        }
    }
}
