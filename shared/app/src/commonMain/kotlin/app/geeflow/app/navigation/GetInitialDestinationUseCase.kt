package app.geeflow.app.navigation

import androidx.navigation3.runtime.NavKey
import app.geeflow.domain.user.usecase.EnsureDefaultUserUseCase
import app.geeflow.navigation.destination.DeviceList
import org.koin.core.annotation.Factory

@Factory
class GetInitialDestinationUseCase(
    private val ensureDefaultUserUseCase: EnsureDefaultUserUseCase,
) {
    operator fun invoke(): NavKey {
        ensureDefaultUserUseCase()
        return DeviceList
    }
}
