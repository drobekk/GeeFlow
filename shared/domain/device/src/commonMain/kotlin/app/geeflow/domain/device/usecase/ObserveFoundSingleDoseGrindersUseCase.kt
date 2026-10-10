package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.SingleDoseGrinder
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class ObserveFoundSingleDoseGrindersUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long): StateFlow<List<SingleDoseGrinder>> =
        provider.getController(deviceId).foundSingleDoseGrinders
}
