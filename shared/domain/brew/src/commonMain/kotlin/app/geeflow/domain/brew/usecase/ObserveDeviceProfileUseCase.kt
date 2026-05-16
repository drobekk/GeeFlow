package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.device.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceProfileUseCase(
    private val deviceRepository: DeviceRepository,
    private val brewProfileRepository: BrewProfileRepository,
) {
    operator fun invoke(deviceId: Long): Flow<BrewProfile?> = combine(
        deviceRepository.devices,
        brewProfileRepository.brewProfiles,
    ) { devices, profiles ->
        val boundProfileId = devices.find { it.id == deviceId }?.boundProfileId
        profiles.find { it.id == boundProfileId }
    }
}
