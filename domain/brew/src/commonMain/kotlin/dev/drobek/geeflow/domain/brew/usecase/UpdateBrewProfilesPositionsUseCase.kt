package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.brew.BrewProfileRepository
import dev.drobek.geeflow.data.brew.model.BrewProfile
import org.koin.core.annotation.Factory

@Factory
class UpdateBrewProfilesPositionsUseCase(
    private val brewProfileRepository: BrewProfileRepository,
) {
    operator fun invoke(profiles: List<BrewProfile>) {
        brewProfileRepository.updateBrewProfilesPositions(profiles)
    }
}
