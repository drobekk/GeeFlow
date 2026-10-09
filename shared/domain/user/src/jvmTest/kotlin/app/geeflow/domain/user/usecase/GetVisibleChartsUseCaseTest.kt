package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.ChartType
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GetVisibleChartsUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when selected user changes then cancels previous settings subscription`() = runTest {
        val firstUserCharts = MutableStateFlow(setOf(ChartType.PRESSURE))
        val secondUserCharts = MutableStateFlow(setOf(ChartType.VOLUME))
        every { settings.visibleCharts(7) } returns firstUserCharts
        every { settings.visibleCharts(8) } returns secondUserCharts
        val useCase = GetVisibleChartsUseCase(settings, users)
        val results = mutableListOf<Set<ChartType>>()

        backgroundScope.launch { useCase().collect { results += it } }
        runCurrent()
        selected.value = User(id = 8, name = "Grace")
        runCurrent()
        firstUserCharts.value = setOf(ChartType.WEIGHT)
        runCurrent()
        secondUserCharts.value = setOf(ChartType.FLOW_RATE)
        runCurrent()

        assertEquals(
            listOf(setOf(ChartType.PRESSURE), setOf(ChartType.VOLUME), setOf(ChartType.FLOW_RATE)),
            results,
        )
    }

    @Test
    fun `when no user is selected then emits nothing and does not read settings`() = runTest {
        selected.value = null
        val useCase = GetVisibleChartsUseCase(settings, users)
        val results = mutableListOf<Set<ChartType>>()

        backgroundScope.launch { useCase().collect { results += it } }
        runCurrent()

        assertEquals(emptyList(), results)
        verify { settings wasNot Called }
    }

    @Test
    fun `when user is selected then combines settings for that user`() = runTest {
        every { settings.visibleCharts(7) } returns flowOf(setOf(ChartType.PRESSURE))
        val useCase = GetVisibleChartsUseCase(settings, users)

        val result = useCase().first()

        assertEquals(setOf(ChartType.PRESSURE), result)
    }

    @Test
    fun `when selected user changes then observes new user settings`() = runTest {
        every { settings.visibleCharts(7) } returns flowOf(setOf(ChartType.PRESSURE))
        val useCase = GetVisibleChartsUseCase(settings, users)
        selected.value = User(id = 8, name = "Grace")
        every { settings.visibleCharts(8) } returns flowOf(setOf(ChartType.PRESSURE))

        val result = useCase().first()

        assertEquals(setOf(ChartType.PRESSURE), result)
        // MockK verifies this call without collecting its returned Flow.
        @Suppress("IgnoredReturnValue")
        verify(exactly = 0) { settings.visibleCharts(7) }
    }
}
