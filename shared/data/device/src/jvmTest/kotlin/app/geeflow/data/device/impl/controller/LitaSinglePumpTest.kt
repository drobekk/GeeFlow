package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.assessProfile
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.SupportedDevice
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LitaSinglePumpTest {
    private val models = listOf(SupportedDevice.LitaBR, SupportedDevice.LitaLR)

    @Test
    fun `when a single pump model is selected then only LR has hot water and neither supports flow control`() = runTest {
        val controllers = models.map { WendougeeController(it, backgroundScope, mockk()) }

        val hotWater = controllers.map { DeviceCapability.HotWaterDispensing in it.capabilities }

        assertEquals(listOf(false, true), hotWater)
        controllers.forEach {
            assertFalse(DeviceCapability.FlowProfiling in it.capabilities)
            assertTrue(DeviceCapability.PressureProfiling in it.capabilities)
            assertNull(it.profilingCapabilities.liveFlow)
            assertEquals(false, it.profilingCapabilities.native?.flow)
            assertTrue(it.profilingCapabilities.livePressure.isNotEmpty())
        }
    }

    @Test
    fun `when BR or LR assesses phases then pressure works and flow including mixed profiles is unsupported`() = runTest {
        val profiles = listOf(
            profile(listOf(ProfileStep.Pressure(5, 6f))),
            profile(listOf(ProfileStep.Flow(5, 2f))),
            profile(listOf(ProfileStep.Pressure(5, 6f), ProfileStep.Flow(5, 2f))),
        )
        val controllers = models.map { WendougeeController(it, backgroundScope, mockk()) }

        val results = controllers.map { controller -> profiles.map { controller.assessProfile(it) } }

        results.forEach { supports ->
            assertEquals(
                listOf(ProfileExecution.Native, ProfileExecution.Unsupported, ProfileExecution.Unsupported),
                supports.map { it.execution },
            )
            assertEquals(listOf(true, false, false), supports.map { it.bindingAllowed })
        }
    }

    @Test
    fun `when BR or LR assesses recordings then only pressure playback can be started or bound`() = runTest {
        val profiles = listOf(FreeHandControlMode.Pressure, FreeHandControlMode.Flow).map { mode ->
            profile(emptyList()).copy(
                program = BrewProgram.Recording(
                    FreeHandRecording(
                        mode,
                        listOf(
                            FreeHandSample(0, BrewDataPoint(4f, 0f, 0f, 2f, 0f)),
                            FreeHandSample(500, BrewDataPoint(6f, 1f, 1f, 3f, 0f)),
                        ),
                    ),
                ),
            )
        }
        val controllers = models.map { WendougeeController(it, backgroundScope, mockk()) }

        val results = controllers.map { controller -> profiles.map { controller.assessProfile(it) } }

        results.forEach { supports ->
            assertEquals(listOf(ProfileExecution.Native, ProfileExecution.Unsupported), supports.map { it.execution })
            assertEquals(listOf(true, false), supports.map { it.bindingAllowed })
        }
    }

    @Test
    fun `when unsupported flow commands reach the controller then rejects them before accessing BLE`() = runTest {
        val transport = mockk<WendougeeBleTransport>()
        val controllers = models.map { WendougeeController(it, backgroundScope, transport) }
        val flowProfile = profile(listOf(ProfileStep.Flow(5, 2f)))

        val results = controllers.flatMap { controller ->
            listOf(
                runCatching { controller.brewing.startFreeVariableBrewing(FreeHandControlMode.Flow) },
                runCatching { controller.brewing.setFreeBrewFlowTarget(2f) },
                runCatching { controller.profiles.startProfileBrewing(flowProfile) },
                runCatching { controller.profiles.bindProfile(flowProfile) },
            )
        }

        results.forEach { assertIs<IllegalArgumentException>(it.exceptionOrNull()) }
        verify { transport wasNot Called }
    }

    private fun profile(steps: List<ProfileStep>) = BrewProfile(
        userId = 1,
        name = "Test",
        description = "",
        finishCondition = Condition.Volume(40f),
        steps = steps,
    )
}
