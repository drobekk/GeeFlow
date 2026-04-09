package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.ChartType
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetVisibleChartsUseCase(
    private val repository: UserSettingsRepository
) {
    operator fun invoke(): Flow<Set<ChartType>> = repository.visibleCharts
}
