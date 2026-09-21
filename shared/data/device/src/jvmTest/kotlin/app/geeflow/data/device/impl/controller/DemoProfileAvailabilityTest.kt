package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewPhase
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseControl
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.data.device.assessProfile
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DemoProfileAvailabilityTest {
    @Test
    fun volumeConditionIsAvailableBeforeFirstBrewAndAfterStopping() = checkAvailability(BrewMetric.PumpedVolume)

    @Test
    fun weightConditionAndFinishTargetRequireAConnectedScale() = checkAvailability(BrewMetric.CupWeight)

    @Test
    fun `native completion retains the volume that reached the target`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val controller = DemoDeviceController(scope = scope)
        val profile = BrewProfile(
            userId = 1,
            name = "Terminal measurement",
            description = "",
            finishCondition = Condition.Volume(target = 1f),
            program = BrewProgram.Phases(
                listOf(BrewPhase(id = "first", control = PhaseControl.Flow(4f), maximumDurationMillis = 10000)),
            ),
        )
        try {
            controller.startProfileBrewing(profile)
            val terminal = withTimeout(3000) {
                controller.deviceState.first { it.brewStatus == DeviceState.BrewStatus.Idle }
            }
            assertTrue(requireNotNull(terminal.volume) >= 1f)
        } finally {
            scope.cancel()
        }
    }

    private fun checkAvailability(metric: BrewMetric) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val controller = DemoDeviceController(scope = scope)
        val profile = BrewProfile(
            userId = 1,
            name = "Demo condition",
            description = "",
            finishCondition = if (metric == BrewMetric.CupWeight) Condition.Weight(target = 36f) else null,
            program = BrewProgram.Phases(
                listOf(
                    BrewPhase(
                        id = "first",
                        control = PhaseControl.Pressure(bar = 3f),
                        maximumDurationMillis = 10000,
                        exitConditions = listOf(
                            ExitCondition(metric = metric, comparison = ThresholdComparison.Above, threshold = 20f),
                        ),
                    ),
                ),
            ),
        )
        try {
            controller.connect(
                Device(
                    name = "Demo",
                    connection = DeviceConnection.Ble(peripheralId = "demo", macAddress = "demo"),
                ),
            )
            withTimeout(3000) {
                controller.deviceState.first { it.connectionStatus == DeviceState.ConnectionStatus.Connected }
            }
            assertNull(controller.telemetry()[BrewMetric.CupWeight])
            if (metric == BrewMetric.CupWeight) {
                assertTrue(controller.assessProfile(profile = profile, checkAvailability = true).issues.isNotEmpty())
                controller.connectSmartScale(name = "Bookoo Themis")
            }
            assertTrue(controller.assessProfile(profile = profile, checkAvailability = true).issues.isEmpty())
            assertEquals(0f, controller.telemetry()[BrewMetric.PumpedVolume])
            assertEquals(0f, controller.telemetry()[BrewMetric.PumpFlow])
            if (metric == BrewMetric.CupWeight) assertEquals(0f, controller.telemetry()[BrewMetric.CupWeight])
            controller.startFreeVariableBrewing(isFlow = false)
            if (metric == BrewMetric.CupWeight) {
                withTimeout(8000) {
                    controller.deviceState.first { (it.weight ?: 0f) > 0f }
                }
                assertTrue(requireNotNull(controller.telemetry()[BrewMetric.CupWeight]) > 0f)
                assertTrue(controller.deviceState.value.smartScale?.isConnected == true)
                assertTrue(controller.assessProfile(profile = profile, checkAvailability = true).issues.isEmpty())
            }
            controller.stopFreeVariableBrewing()
            assertTrue(controller.assessProfile(profile = profile, checkAvailability = true).issues.isEmpty())
            controller.disconnect()
            assertNull(controller.telemetry()[BrewMetric.PumpedVolume])
            assertNull(controller.telemetry()[BrewMetric.CupWeight])
        } finally {
            scope.cancel()
        }
    }
}
