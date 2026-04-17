package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.DeviceConstraints
import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.TemperatureUnit
import dev.drobek.geeflow.domain.celsiusToFahrenheit
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import kotlin.math.roundToInt

@Factory
class GetDeviceConstraintsUseCase(
    private val provider: DeviceControllerProvider,
    private val userSettingsRepository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(deviceId: Long): DeviceConstraints {
        val userId = userRepository.selectedUser.first()?.id ?: return provider.getController(deviceId).constraints
        val constraints = provider.getController(deviceId).constraints
        val unit = userSettingsRepository.temperatureUnit(userId).first()
        return if (unit == TemperatureUnit.FAHRENHEIT) {
            constraints.copy(
                brewTempRange = constraints.brewTempRange.toFahrenheit(),
                steamTempRange = constraints.steamTempRange.toFahrenheit(),
            )
        } else {
            constraints
        }
    }

    private fun IntRange.toFahrenheit(): IntRange =
        first.toFloat().celsiusToFahrenheit().roundToInt()..last.toFloat().celsiusToFahrenheit().roundToInt()
}
