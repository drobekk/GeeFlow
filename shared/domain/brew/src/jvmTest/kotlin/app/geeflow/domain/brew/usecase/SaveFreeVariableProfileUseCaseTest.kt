package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.exception.RecordingNoVolumeException
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SaveFreeVariableProfileUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val getSelected = GetSelectedUserUseCase(users)
    private val point = BrewDataPoint(9f, 36f, 40f, 2f, 1f)

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when recording has volume then stores recording and volume finish condition`() = runTest {
        val recording = FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(0, point)))
        val session = BrewSession(userId = 1, recording = recording)
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)

        useCase(session, "Recorded")

        verify(
            exactly = 1
        ) {
            profiles.addBrewProfile(
                BrewProfile(
                    userId = 7,
                    name = "Recorded",
                    description = "",
                    finishCondition = Condition.Volume(40f),
                    program = BrewProgram.Recording(recording)
                )
            )
        }
    }

    @Test
    fun `when recording has no volume then rejects saving`() = runTest {
        val recording =
            FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(0, point.copy(volume = 0f))))
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)

        val result = runCatching { useCase(BrewSession(userId = 7, recording = recording), "Recorded") }

        assertTrue(result.exceptionOrNull() is RecordingNoVolumeException)
        verify(exactly = 0) { profiles.addBrewProfile(any()) }
    }
}
