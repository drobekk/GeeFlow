package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.DeviceState.BoilerType
import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.TemperatureUnit
import dev.drobek.geeflow.domain.fahrenheitToCelsius
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import kotlin.math.roundToInt

@Factory
class SetBoilerSettingsUseCase(
    private val provider: DeviceControllerProvider,
    private val userSettingsRepository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        deviceId: Long,
        boilerType: BoilerType,
        enabled: Boolean,
        temp: Int,
    ) {
        val userId = userRepository.selectedUser.first()?.id ?: return
        val unit = userSettingsRepository.temperatureUnit(userId).first()
        val tempCelsius = if (unit == TemperatureUnit.FAHRENHEIT) {
            temp.toFloat().fahrenheitToCelsius().roundToInt()
        } else {
            temp
        }
        with(provider.getController(deviceId)) {
            requireConnected(deviceId)
            setBoilerState(boilerType, enabled)
            when (boilerType) {
                BoilerType.Brew -> setBrewTemperature(tempCelsius)
                BoilerType.Steam -> setSteamTemperature(tempCelsius)
            }
        }
    }
}
