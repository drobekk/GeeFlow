package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.DeviceState
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.TemperatureUnit
import dev.drobek.geeflow.domain.celsiusToFahrenheit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceStateUseCase(
    private val provider: DeviceControllerProvider,
    private val userSettingsRepository: UserSettingsRepository,
) {
    operator fun invoke(deviceId: Long): Flow<DeviceState> =
        provider.getController(deviceId).deviceState
            .combine(userSettingsRepository.temperatureUnit) { state, unit ->
                if (unit == TemperatureUnit.FAHRENHEIT) state.toFahrenheit() else state
            }

    private fun DeviceState.toFahrenheit() = copy(
        brewBoilerTemp = brewBoilerTemp?.celsiusToFahrenheit(),
        steamBoilerTemp = steamBoilerTemp?.celsiusToFahrenheit(),
        config = config?.let {
            it.copy(
                targetBrewTemp = it.targetBrewTemp.celsiusToFahrenheit(),
                targetSteamTemp = it.targetSteamTemp.celsiusToFahrenheit(),
            )
        },
    )
}
