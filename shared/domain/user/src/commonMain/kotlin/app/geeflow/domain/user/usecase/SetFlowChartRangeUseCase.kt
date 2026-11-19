package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.FlowChartRange
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetFlowChartRangeUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(range: FlowChartRange) {
        userRepository.selectedUser.first()?.id?.let { userId ->
            repository.setFlowChartRange(userId, range)
        }
    }
}
