package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import org.koin.core.annotation.Factory

@Factory
class GetBrewProfileUseCase(
    private val brewProfileRepository: BrewProfileRepository,
) {
    operator fun invoke(id: Long): BrewProfile? = brewProfileRepository.getBrewProfileById(id)
}
