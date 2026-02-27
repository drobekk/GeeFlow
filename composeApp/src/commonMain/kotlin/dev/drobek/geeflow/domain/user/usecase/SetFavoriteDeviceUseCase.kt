package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.users.api.UserRepository
import org.koin.core.annotation.Factory

@Factory
class SetFavoriteDeviceUseCase(
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: Long, deviceMacAddress: String?) {
        userRepository.setFavoriteDevice(userId, deviceMacAddress)
    }
}
