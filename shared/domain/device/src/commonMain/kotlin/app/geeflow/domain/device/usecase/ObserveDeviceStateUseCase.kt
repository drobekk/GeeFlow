package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.domain.celsiusToFahrenheit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceStateUseCase(
    private val provider: DeviceControllerProvider,
    private val userSettingsRepository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(deviceId: Long): Flow<DeviceState> = userRepository.selectedUser
        .mapNotNull { it?.id }
        .flatMapLatest { userId ->
            provider.getController(deviceId).deviceState
                .combine(userSettingsRepository.temperatureUnit(userId)) { state, unit ->
                    if (unit == TemperatureUnit.FAHRENHEIT) state.toFahrenheit() else state
                }
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
