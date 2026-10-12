package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.SingleDoseGrinder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import org.koin.core.annotation.Factory

@Factory
class ObserveFoundSingleDoseGrindersUseCase(private val provider: DeviceControllerProvider) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(deviceId: Long): Flow<List<SingleDoseGrinder>> =
        provider.observeController(deviceId).flatMapLatest { it.foundSingleDoseGrinders }
}
