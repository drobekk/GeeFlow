package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.user.UserRepository
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.exception.ScaleNotConnectedException
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class StartProfileBrewingUseCase(
    private val provider: DeviceControllerProvider,
    private val userRepository: UserRepository,
    private val coordinator: ProfileExecutionCoordinator
) {
    suspend operator fun invoke(deviceId: Long, profile: BrewProfile) = with(provider.getController(deviceId)) {
        requireConnected(deviceId)
        if (profile.finishCondition is Condition.Weight && deviceState.value.smartScale?.isConnected != true) {
            throw ScaleNotConnectedException(deviceId)
        }
        val userId = userRepository.selectedUser.first()?.id ?: error("No user selected")
        coordinator.start(deviceId, profile.copy(userId = userId))
    }
}
