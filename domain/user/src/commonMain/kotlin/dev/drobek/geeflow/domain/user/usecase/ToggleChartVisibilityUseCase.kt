package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.ChartType
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class ToggleChartVisibilityUseCase(
    private val repository: UserSettingsRepository,
) {
    suspend operator fun invoke(type: ChartType) {
        val currentSet = repository.visibleCharts.first()
        val newSet = if (currentSet.contains(type)) {
            currentSet - type
        } else {
            currentSet + type
        }

        repository.setVisibleCharts(newSet)
    }
}
