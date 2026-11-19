package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.koin.core.annotation.Factory

@Factory
class GetFavoriteProfilesUseCase(private val repository: UserSettingsRepository, private val users: UserRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Set<String>> = users.selectedUser.flatMapLatest { user ->
        user?.id?.let(repository::favoriteProfiles) ?: flowOf(emptySet())
    }
}
