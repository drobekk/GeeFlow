package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class SetFavoriteDeviceUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(userId: Long, deviceId: Long?) {
        userRepository.setFavoriteDevice(userId, deviceId)
    }
}
