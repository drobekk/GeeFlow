package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory
class GetPreferredShotDoseUseCase(
    private val settings: UserSettingsRepository,
    private val users: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Float> = users.selectedUser.map { it?.id }.flatMapLatest { userId ->
        if (userId == null) flowOf(DEFAULT_SHOT_DOSE_GRAMS) else settings.preferredShotDoseGrams(userId)
    }

    private companion object { const val DEFAULT_SHOT_DOSE_GRAMS = 18f }
}
