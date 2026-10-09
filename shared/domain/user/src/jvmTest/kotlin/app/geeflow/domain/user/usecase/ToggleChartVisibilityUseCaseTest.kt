package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.ChartType
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ToggleChartVisibilityUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when chart is visible then removes chart for selected user`() = runTest {
        every { settings.visibleCharts(7) } returns flowOf(setOf(ChartType.PRESSURE, ChartType.VOLUME))
        val useCase = ToggleChartVisibilityUseCase(settings, users)

        useCase(ChartType.PRESSURE)

        coVerify(exactly = 1) { settings.setVisibleCharts(7, setOf(ChartType.VOLUME)) }
    }

    @Test
    fun `when chart is hidden then adds chart for selected user`() = runTest {
        every { settings.visibleCharts(7) } returns flowOf(setOf(ChartType.VOLUME))
        val useCase = ToggleChartVisibilityUseCase(settings, users)

        useCase(ChartType.PRESSURE)

        coVerify(exactly = 1) { settings.setVisibleCharts(7, setOf(ChartType.VOLUME, ChartType.PRESSURE)) }
    }

    @Test
    fun `when no user is selected then does not change charts`() = runTest {
        selected.value = null
        val useCase = ToggleChartVisibilityUseCase(settings, users)

        useCase(ChartType.PRESSURE)

        coVerify { settings wasNot Called }
    }
}
