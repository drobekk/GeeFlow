package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository
) {
    operator fun invoke(deviceMac: String): Flow<BrewProfile?> {
        return brewProfileRepository.observeBrewProfileForDevice(deviceMac)
    }
}
