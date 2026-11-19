package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.CaptureAction
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.TargetSource
import app.geeflow.data.brew.model.TargetSourceKind
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.BookooScaleReading
import app.geeflow.data.device.BookooScaleState
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.ScaleReadingSource
import app.geeflow.data.device.model.BookooScaleFrame
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SmartScale
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.device.AppWeightCutoff
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.WeightCutoffSource
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartProfileBrewingUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxed = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val scale = mockk<ScaleReadingSource>(relaxed = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { users.selectedUser } returns selected
        every { scale.state } returns MutableStateFlow(BookooScaleState())
    }

    @Test
    fun `when profile starts then coordinator receives selected user id`() = runTest {
        val profile = BrewProfile(
            userId = 1,
            name = "Shot",
            description = "",
            finishCondition = null,
            steps = listOf(ProfileStep.Pressure(30, 9f))
        )
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, profile)

        coVerify(exactly = 1) { coordinator.start(9, profile.copy(userId = 7), null) }
    }

    @Test
    fun `weight finish falls back to the programmed recipe when no scale is available`() = runTest {
        val profile = BrewProfile(
            userId = 7,
            name = "Shot",
            description = "",
            finishCondition = Condition.Weight(36f),
            steps = listOf(ProfileStep.Pressure(30, 9f))
        )
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, profile)

        coVerify(exactly = 1) {
            coordinator.start(9, profile.copy(userId = 7, finishCondition = null), null)
        }
    }

    @Test
    fun `scale dependent phase rules fall back to their programmed time limits`() = runTest {
        val profile = BrewProfile(
            userId = 7,
            name = "Scale adaptive",
            description = "",
            finishCondition = Condition.Weight(36f),
            program = BrewProgram.Phases(
                version = 2,
                phases = listOf(
                    BrewPhase(
                        id = "bloom",
                        control = PhaseControl.Pressure(3f),
                        maximumDurationMillis = 5_000,
                        exitConditions = listOf(
                            ExitCondition(BrewMetric.CupWeight, ThresholdComparison.Above, 2f),
                        ),
                        targetSource = TargetSource(
                            kind = TargetSourceKind.MeasurementAtEntry,
                            metric = BrewMetric.CupWeight,
                        ),
                        ramp = PhaseRamp(
                            style = RampStyle.Linear,
                            durationMillis = 1_000,
                            source = TargetSource(
                                kind = TargetSourceKind.MeasurementAtEntry,
                                metric = BrewMetric.CupWeight,
                            ),
                        ),
                        entryCaptures = listOf(CaptureAction("yield", BrewMetric.CupWeight, fallback = 2f)),
                    ),
                    BrewPhase(
                        id = "finish",
                        control = PhaseControl.Pressure(6f),
                        maximumDurationMillis = 8_000,
                        targetSource = TargetSource(kind = TargetSourceKind.CapturedValue, capture = "yield"),
                    ),
                ),
            ),
        )
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, profile)

        val runtime = slot<BrewProfile>()
        coVerify(exactly = 1) { coordinator.start(9, capture(runtime), null) }
        val runtimeProgram = runtime.captured.program as BrewProgram.Phases
        assertEquals(null, runtime.captured.finishCondition)
        assertEquals(emptyList(), runtimeProgram.phases[0].exitConditions)
        assertEquals(null, runtimeProgram.phases[0].targetSource)
        assertEquals(null, runtimeProgram.phases[0].ramp.source)
        assertEquals(emptyList(), runtimeProgram.phases[0].entryCaptures)
        assertEquals(null, runtimeProgram.phases[1].targetSource)
        assertEquals(5_000, runtimeProgram.phases[0].maximumDurationMillis)
        assertEquals(8_000, runtimeProgram.phases[1].maximumDurationMillis)
    }

    @Test
    fun `yield cutoff uses a fresh tared directly connected BOOKOO when machine scale is not ready`() = runTest {
        val profile = volumeProfile()
        val reading = BookooScaleReading(BookooScaleFrame(1_000, 0f, 0f, 100), Clock.System.now(), "")
        every { scale.state } returns MutableStateFlow(BookooScaleState(connected = true, reading = reading))
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, profile, stopAtWeightGrams = 30f)

        val cutoff = slot<AppWeightCutoff>()
        coVerify(exactly = 1) { coordinator.start(9, profile.copy(userId = 7), capture(cutoff)) }
        assertEquals(30f, cutoff.captured.targetGrams)
        assertEquals(WeightCutoffSource.Bookoo, cutoff.captured.source)
    }

    @Test
    fun `BOOKOO cutoff can replace a recipe weight finish without a machine scale`() = runTest {
        val reading = BookooScaleReading(BookooScaleFrame(1_000, 0f, 0f, 100), Clock.System.now(), "")
        every { scale.state } returns MutableStateFlow(BookooScaleState(connected = true, reading = reading))
        val weightProfile = volumeProfile().copy(finishCondition = Condition.Weight(60f))
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, weightProfile, stopAtWeightGrams = 30f)

        val cutoff = slot<AppWeightCutoff>()
        coVerify(exactly = 1) { coordinator.start(9, weightProfile.copy(userId = 7), capture(cutoff)) }
        assertEquals(30f, cutoff.captured.targetGrams)
        assertEquals(WeightCutoffSource.Bookoo, cutoff.captured.source)
    }

    @Test
    fun `stale scale data disables the cutoff and still starts the recipe`() = runTest {
        val reading = BookooScaleReading(
            BookooScaleFrame(1_000, 0f, 0f, 100),
            Clock.System.now() - 1_500.milliseconds,
            "",
        )
        every { scale.state } returns MutableStateFlow(BookooScaleState(connected = true, reading = reading))
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        val weightStopUnavailable = useCase(9, volumeProfile(), stopAtWeightGrams = 30f)

        coVerify(exactly = 1) { coordinator.start(9, volumeProfile().copy(userId = 7), null) }
        assertTrue(weightStopUnavailable)
    }

    @Test
    fun `untared scale disables the cutoff and still starts the recipe`() = runTest {
        val reading = BookooScaleReading(BookooScaleFrame(1_000, 2.5f, 0f, 100), Clock.System.now(), "")
        every { scale.state } returns MutableStateFlow(BookooScaleState(connected = true, reading = reading))
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, volumeProfile(), stopAtWeightGrams = 30f)

        coVerify(exactly = 1) { coordinator.start(9, volumeProfile().copy(userId = 7), null) }
    }

    @Test
    fun `ready machine paired scale takes priority over direct BOOKOO`() = runTest {
        state.value = DeviceState(
            connectionStatus = DeviceState.ConnectionStatus.Connected,
            smartScale = SmartScale("Machine scale", true),
            weight = 0f,
            telemetryTime = Clock.System.now(),
        )
        every { scale.state } returns MutableStateFlow(BookooScaleState())
        val profile = volumeProfile()
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, profile, stopAtWeightGrams = 30f)

        val cutoff = slot<AppWeightCutoff>()
        coVerify(exactly = 1) { coordinator.start(9, profile.copy(userId = 7), capture(cutoff)) }
        assertEquals(30f, cutoff.captured.targetGrams)
        assertEquals(WeightCutoffSource.MachineScale, cutoff.captured.source)
    }

    @Test
    fun `untared machine paired scale falls back to fresh tared BOOKOO`() = runTest {
        state.value = DeviceState(
            connectionStatus = DeviceState.ConnectionStatus.Connected,
            smartScale = SmartScale("Machine scale", true),
            weight = 3f,
            telemetryTime = Clock.System.now(),
        )
        val reading = BookooScaleReading(BookooScaleFrame(1_000, 0f, 0f, 100), Clock.System.now(), "")
        every { scale.state } returns MutableStateFlow(BookooScaleState(connected = true, reading = reading))
        val profile = volumeProfile()
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, profile, stopAtWeightGrams = 30f)

        val cutoff = slot<AppWeightCutoff>()
        coVerify(exactly = 1) { coordinator.start(9, profile.copy(userId = 7), capture(cutoff)) }
        assertEquals(WeightCutoffSource.Bookoo, cutoff.captured.source)
    }

    @Test
    fun `connected scale without data still starts the recipe without a cutoff`() = runTest {
        every { scale.state } returns MutableStateFlow(BookooScaleState(connected = true))
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator, scale)

        useCase(9, volumeProfile(), stopAtWeightGrams = 30f)

        coVerify(exactly = 1) { coordinator.start(9, volumeProfile().copy(userId = 7), null) }
    }

    private fun volumeProfile() = BrewProfile(
        userId = 7,
        name = "Volume shot",
        description = "",
        finishCondition = Condition.Volume(60f),
        steps = listOf(ProfileStep.Pressure(30, 9f)),
    )
}
