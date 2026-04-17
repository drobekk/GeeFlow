package dev.drobek.geeflow.domain.user.usecase

import dev.drobek.geeflow.data.user.UserRepository
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.ChartType
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class ToggleChartVisibilityUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(type: ChartType) {
        val userId = userRepository.selectedUser.first()?.id ?: return
        val currentSet = repository.visibleCharts(userId).first()
        val newSet = if (currentSet.contains(type)) currentSet - type else currentSet + type
        repository.setVisibleCharts(userId, newSet)
    }
}
