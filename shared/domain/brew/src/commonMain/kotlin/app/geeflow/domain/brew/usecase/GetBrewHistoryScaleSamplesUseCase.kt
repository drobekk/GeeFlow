package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewScaleSample
import org.koin.core.annotation.Factory

@Factory
class GetBrewHistoryScaleSamplesUseCase(private val repository: BrewHistoryRepository) {
    operator fun invoke(id: Long): List<BrewScaleSample> = repository.getBrewScaleSamples(id)
}
