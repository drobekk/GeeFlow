package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class DeleteDeviceUseCase(
    private val maintenance: MaintenanceSettingsRepository,
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository,
    private val controllerProvider: DeviceControllerProvider,
) {
    suspend operator fun invoke(id: Long) {
        maintenance.remove(id)
        if (controllerProvider.currentDeviceId == id) {
            controllerProvider.disconnectCurrent()
        }
        deviceRepository.removeDeviceById(id)
        userRepository.users.value
            .filter { it.favoriteDeviceId == id }
            .forEach { userRepository.setFavoriteDevice(it.id, null) }
    }
}
