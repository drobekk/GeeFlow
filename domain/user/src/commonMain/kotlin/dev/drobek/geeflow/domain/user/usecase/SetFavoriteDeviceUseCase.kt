package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import org.koin.core.annotation.Factory

@Factory
class SetFavoriteDeviceUseCase(
    private val userRepository: UserRepository,
) {
    operator fun invoke(userId: Long, deviceId: Long?) {
        userRepository.setFavoriteDevice(userId, deviceId)
    }
}
