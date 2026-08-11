package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.user.UserRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class GetBrewHistoryUseCase(
    private val brewHistoryRepository: BrewHistoryRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(query: String, limit: Int, offset: Int): List<BrewHistoryEntry> {
        val userId = userRepository.selectedUser.first()?.id ?: return emptyList()
        return brewHistoryRepository.getBrews(userId, query, limit, offset)
    }
}
