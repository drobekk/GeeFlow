package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.api.BrewProfileRepository
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import org.koin.core.annotation.Factory

@Factory
class GetBrewProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository
) {
    operator fun invoke(id: Long): BrewProfile? {
        return brewProfileRepository.getBrewProfileById(id)
    }
}
