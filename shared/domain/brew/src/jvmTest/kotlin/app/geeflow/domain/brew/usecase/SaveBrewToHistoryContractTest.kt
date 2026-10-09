package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.model.DeviceBrewingSettings
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

class SaveBrewToHistoryContractTest {
    private val history = mockk<BrewHistoryRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val brewing = mockk<DeviceBrewingSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val profile = BrewProfile(
        id = 3,
        userId = 7,
        name = "Shot",
        description = "Recipe",
        finishCondition = null,
        steps = listOf(ProfileStep.Pressure(30, 9f)),
        autoLinkOpen = true,
        position = 4
    )
    private val point = BrewDataPoint(9f, 36f, 40f, 2f, 1f)

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when profile session is complete then stores metadata and telemetry for selected user`() = runTest {
        val session = BrewSession(
            userId = 1,
            mode = BrewMode.Profile,
            startTime = Instant.fromEpochSeconds(100),
            elapsedSeconds = 30,
            dataPoints = mapOf(0f to point)
        )
        val useCase = SaveBrewToHistoryUseCase(history, brewing, users)

        useCase(9, session, 3, "Shot", profile.steps)

        verify(
            exactly = 1
        ) {
            history.addBrew(
                BrewHistoryEntry(
                    userId = 7,
                    profileId = 3,
                    profileName = "Shot",
                    mode = BrewMode.Profile,
                    startedAt = Instant.fromEpochSeconds(100),
                    durationSeconds = 30,
                    profileSteps = profile.steps
                ),
                session.dataPoints
            )
        }
    }

    @Test
    fun `when session has no telemetry then skips history`() = runTest {
        val session = BrewSession(
            userId = 1,
            mode = BrewMode.Profile,
            startTime = Instant.fromEpochSeconds(100),
            elapsedSeconds = 30,
            dataPoints = mapOf(0f to point)
        )
        val useCase = SaveBrewToHistoryUseCase(history, brewing, users)

        useCase(9, session.copy(dataPoints = emptyMap()), 3, "Shot")

        verify { history wasNot Called }
    }

    @Test
    fun `when session has no start time then skips history`() = runTest {
        val session = BrewSession(
            userId = 1,
            mode = BrewMode.Profile,
            startTime = Instant.fromEpochSeconds(100),
            elapsedSeconds = 30,
            dataPoints = mapOf(0f to point)
        )
        val useCase = SaveBrewToHistoryUseCase(history, brewing, users)

        useCase(9, session.copy(startTime = null), 3, "Shot")

        verify { history wasNot Called }
    }

    @Test
    fun `when session has execution trace then leaves persistence to coordinator`() = runTest {
        val session = BrewSession(
            userId = 1,
            mode = BrewMode.Profile,
            startTime = Instant.fromEpochSeconds(100),
            elapsedSeconds = 30,
            dataPoints = mapOf(0f to point)
        )
        val useCase = SaveBrewToHistoryUseCase(history, brewing, users)

        useCase(
            9,
            session.copy(executionTrace = ProfileExecutionTrace(profile, Instant.fromEpochSeconds(100))),
            3,
            "Shot",
        )

        verify { history wasNot Called }
    }

    @Test
    fun `when no user is selected then skips history`() = runTest {
        val session = BrewSession(
            userId = 1,
            mode = BrewMode.Profile,
            startTime = Instant.fromEpochSeconds(100),
            elapsedSeconds = 30,
            dataPoints = mapOf(0f to point)
        )
        val useCase = SaveBrewToHistoryUseCase(history, brewing, users)
        selected.value = null

        useCase(9, session, 3, "Shot")

        verify { history wasNot Called }
    }

    @Test
    fun `when manual brew is treated as flush then skips history`() = runTest {
        val session = BrewSession(
            userId = 1,
            mode = BrewMode.Profile,
            startTime = Instant.fromEpochSeconds(100),
            elapsedSeconds = 30,
            dataPoints = mapOf(0f to point)
        )
        val useCase = SaveBrewToHistoryUseCase(history, brewing, users)
        every { brewing.observe(9) } returns flowOf(DeviceBrewingSettings(treatManualAsFlush = true))

        useCase(9, session.copy(mode = BrewMode.Manual), null, null)

        verify { history wasNot Called }
    }

    @Test
    fun `when manual brew is not treated as flush then saves history`() = runTest {
        val session = BrewSession(
            userId = 1,
            mode = BrewMode.Profile,
            startTime = Instant.fromEpochSeconds(100),
            elapsedSeconds = 30,
            dataPoints = mapOf(0f to point)
        )
        val useCase = SaveBrewToHistoryUseCase(history, brewing, users)
        every { brewing.observe(9) } returns flowOf(DeviceBrewingSettings(treatManualAsFlush = false))

        useCase(9, session.copy(mode = BrewMode.Manual), null, null)

        verify(exactly = 1) {
            history.addBrew(match { it.userId == 7L && it.mode == BrewMode.Manual }, session.dataPoints)
        }
    }
}
