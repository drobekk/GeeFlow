package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import org.koin.core.annotation.Factory

@Factory
class GetBrewHistoryDataUseCase(
    private val brewHistoryRepository: BrewHistoryRepository,
) {
    operator fun invoke(id: Long): Map<Float, BrewDataPoint> = brewHistoryRepository.getBrewDataPoints(id)
}
