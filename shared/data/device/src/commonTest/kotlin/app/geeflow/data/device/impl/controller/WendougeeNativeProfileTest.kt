package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.assessProfile
import app.geeflow.data.device.ble.modbus.ModbusCrcException
import app.geeflow.data.device.ble.modbus.ModbusProtocolException
import app.geeflow.data.device.model.ProfileExecution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WendougeeNativeProfileTest {
    private val compiler = WendougeeProfileCompiler()

    @Test
    fun `when recording contains measured weights then bank words use whole grams`() {
        val profile = recordingProfile()

        val writes = compiler.buildProfileWrites(profile)
        val bank = writes.filterIsInstance<ProfileWrite.Multiple>().first { it.register == 1016 }

        assertEquals(listOf(12, 18), bank.values.take(2))
        assertEquals(18, bank.values.last())
    }

    @Test
    fun `when recording is bound then all shortcut parameters use the second key`() {
        val profile = recordingProfile().copy(autoLinkOpen = true)

        val writes = compiler.buildProfileWrites(profile, isBinding = true)
        val commitCoil = WendougeeProfileCompiler.profileCommitCoil(isBinding = true)

        assertTrue(ProfileWrite.Multiple(80, listOf(0)) in writes)
        assertTrue(ProfileWrite.Multiple(88, listOf(4)) in writes)
        assertTrue(ProfileWrite.Single(363, 0) in writes)
        assertTrue(ProfileWrite.Single(359, 36) in writes)
        assertTrue(ProfileWrite.Single(367, 1) in writes)
        assertFalse(writes.any { it.register in listOf(79, 87, 362, 358, 366) })
        assertEquals(159, commitCoil)
    }

    @Test
    fun `when recording starts from the app then shortcut parameters use the first key`() {
        val profile = recordingProfile()

        val writes = compiler.buildProfileWrites(profile)
        val commitCoil = WendougeeProfileCompiler.profileCommitCoil(isBinding = false)

        assertTrue(ProfileWrite.Multiple(79, listOf(0)) in writes)
        assertTrue(ProfileWrite.Multiple(87, listOf(4)) in writes)
        assertTrue(ProfileWrite.Single(362, 0) in writes)
        assertTrue(ProfileWrite.Single(358, 36) in writes)
        assertTrue(ProfileWrite.Single(366, 0) in writes)
        assertEquals(158, commitCoil)
    }

    @Test
    fun `when the native phase profile uses weight then mode remains two`() {
        val profile = phaseProfile(listOf(ProfileStep.Pressure(5, 6f)))

        val writes = compiler.buildProfileWrites(profile)

        assertTrue(ProfileWrite.Single(87, 2) in writes)
        assertTrue(ProfileWrite.Multiple(2048, listOf(0, 1, 1, 1, 0, 36, 0)) in writes)
    }

    @Test
    fun `when a profile ends with a pause then native execution is rejected without discarding the pause`() {
        val profile = phaseProfile(listOf(ProfileStep.Pressure(5, 6f), ProfileStep.Wait(3)))

        val issues = WendougeeProfiling.assessNative(profile)
        val result = runCatching { compiler.buildProfileWrites(profile) }

        assertEquals(listOf("1"), issues.map { it.phaseId })
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertEquals(ProfileStep.Wait(3), profile.steps.last())
    }

    @Test
    fun `when a pause separates active phases then it is encoded on the preceding native step`() {
        val profile = phaseProfile(listOf(ProfileStep.Pressure(5, 6f), ProfileStep.Wait(3), ProfileStep.Flow(4, 2f)))

        val writes = compiler.buildProfileWrites(profile)

        assertTrue(ProfileWrite.Multiple(2056, listOf(5, 60, 0, 3, 0, 0)) in writes)
        assertTrue(ProfileWrite.Multiple(2065, listOf(4, 0, 20, 0, 1, 1)) in writes)
    }

    @Test
    fun `when a profile ends with a pause then application control preserves the requested program`() = runTest {
        val profile = phaseProfile(listOf(ProfileStep.Pressure(5, 6f), ProfileStep.Wait(3)))
        val controller = object : DeviceController by DemoDeviceController(backgroundScope) {
            override val profilingCapabilities = WendougeeProfiling.capabilities
            override fun assessNativeProfile(profile: BrewProfile) = WendougeeProfiling.assessNative(profile)
        }

        val support = controller.assessProfile(profile)

        assertEquals(ProfileExecution.AppControlled, support.execution)
        assertFalse(support.bindingAllowed)
    }

    @Test
    fun `when a profile register write has transient CRC errors then it succeeds within five attempts`() = runTest {
        var attempts = 0
        val attemptTimes = mutableListOf<Long>()

        writeProfileWithRetry {
            attempts++
            attemptTimes += currentTime
            if (attempts < 5) throw ModbusCrcException("corrupt response")
        }

        assertEquals(5, attempts)
        assertEquals(listOf(0L, 50L, 150L, 300L, 500L), attemptTimes)
    }

    @Test
    fun `when a profile register write keeps failing then the fifth error is returned`() = runTest {
        val failure = ModbusCrcException("corrupt response")
        var attempts = 0

        val result = runCatching {
            writeProfileWithRetry {
                attempts++
                throw failure
            }
        }

        assertEquals(5, attempts)
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `when a profile write times out then a later attempt may succeed`() = runTest {
        var attempts = 0

        writeProfileWithRetry {
            attempts++
            if (attempts == 1) withTimeout(1) { delay(2) }
        }

        assertEquals(2, attempts)
    }

    @Test
    fun `when transport temporarily rejects a profile write then the register write is retried`() = runTest {
        var attempts = 0

        writeProfileWithRetry {
            attempts++
            if (attempts == 1) error("Transport temporarily unavailable")
        }

        assertEquals(2, attempts)
    }

    @Test
    fun `when the device rejects a profile register write then it is not retried`() = runTest {
        val failure = ModbusProtocolException(0x90.toByte(), 2)
        var attempts = 0

        val result = runCatching {
            writeProfileWithRetry {
                attempts++
                throw failure
            }
        }

        assertEquals(1, attempts)
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `when profile upload is cancelled then it is not retried`() = runTest {
        val failure = CancellationException("cancelled")
        var attempts = 0

        val result = runCatching {
            writeProfileWithRetry {
                attempts++
                throw failure
            }
        }

        assertEquals(1, attempts)
        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `when the enclosing upload times out then no subsequent writes are attempted`() = runTest {
        var attempts = 0

        val result = runCatching {
            withTimeout(1) {
                writeProfileWithRetry {
                    attempts++
                    delay(2)
                }
            }
        }

        assertEquals(1, attempts)
        assertIs<CancellationException>(result.exceptionOrNull())
    }

    private fun recordingProfile(): BrewProfile = BrewProfile(
        userId = 1,
        name = "Measured recording",
        description = "",
        finishCondition = Condition.Weight(36f),
        program = BrewProgram.Recording(
            FreeHandRecording(
                FreeHandControlMode.Pressure,
                listOf(
                    FreeHandSample(0, BrewDataPoint(4f, 12.3f, 15f, 2f, 0f)),
                    FreeHandSample(500, BrewDataPoint(6f, 18.7f, 20f, 3f, 0f)),
                ),
            ),
        ),
    )

    private fun phaseProfile(steps: List<ProfileStep>): BrewProfile = BrewProfile(
        userId = 1,
        name = "Native phases",
        description = "",
        finishCondition = Condition.Weight(36f),
        steps = steps,
    )
}
