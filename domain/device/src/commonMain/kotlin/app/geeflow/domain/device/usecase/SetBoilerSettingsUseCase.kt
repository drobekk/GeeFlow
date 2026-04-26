package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.domain.fahrenheitToCelsius
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
