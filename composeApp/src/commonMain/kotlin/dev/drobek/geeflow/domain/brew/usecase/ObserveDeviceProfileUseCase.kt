package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import dev.drobek.geeflow.data.device.api.DeviceRepository
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceProfileUseCase(
    private val deviceRepository: DeviceRepository,
    private val brewProfileRepository: BrewProfileRepository
) {
    operator fun invoke(deviceMac: String): Flow<BrewProfile?> = combine(
        deviceRepository.devices,
        brewProfileRepository.brewProfiles
    ) { devices, profiles ->
        val boundProfileId = devices.find { it.macAddress == deviceMac }?.boundProfileId
        profiles.find { it.id == boundProfileId }
    }
}
