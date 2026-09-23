package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.model.DeviceState.BrewStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoNativeCompletionTest {
    @Test
    fun `volume target continues past the planned duration until 100 ml is reached`() = runTest {
        val controller = DemoDeviceController(scope = backgroundScope)
        controller.startProfileBrewing(profile(Condition.Volume(100f)))
        delay(30000)
        assertEquals(BrewStatus.Profile, controller.deviceState.value.brewStatus)
        assertTrue(requireNotNull(controller.deviceState.value.volume) < 100f)
        val finished = withTimeout(10000) {
            controller.deviceState.first { it.brewStatus == BrewStatus.Idle }
        }
        assertTrue(requireNotNull(finished.volume) >= 100f)
        assertEquals(finished.telemetryTime, finished.statusTime)
    }

    @Test
    fun `weight target continues past the planned duration until 100 g is reached`() = runTest {
        val controller = DemoDeviceController(scope = backgroundScope)
        controller.startProfileBrewing(profile(Condition.Weight(100f)))
        delay(30000)
        assertEquals(BrewStatus.Profile, controller.deviceState.value.brewStatus)
        assertTrue(requireNotNull(controller.deviceState.value.weight) < 100f)
        val finished = withTimeout(20000) {
            controller.deviceState.first { it.brewStatus == BrewStatus.Idle }
        }
        assertTrue(requireNotNull(finished.weight) >= 100f)
    }

    @Test
    fun `a profile without a finish target stops at the planned duration`() = runTest {
        val controller = DemoDeviceController(scope = backgroundScope)
        controller.startProfileBrewing(profile(null))
        withTimeout(31000) {
            controller.deviceState.first { it.brewStatus == BrewStatus.Idle }
        }
        assertEquals(BrewStatus.Idle, controller.deviceState.value.brewStatus)
    }

    @Test
    fun `manual stop still interrupts a profile waiting for its target`() = runTest {
        val controller = DemoDeviceController(scope = backgroundScope)
        controller.startProfileBrewing(profile(Condition.Volume(100f)))
        delay(30000)
        controller.stopProfileBrewing()
        delay(5000)
        assertEquals(BrewStatus.Idle, controller.deviceState.value.brewStatus)
    }

    private fun profile(condition: Condition?) = BrewProfile(
        userId = 1,
        name = "Native completion",
        description = "",
        finishCondition = condition,
        steps = listOf(ProfileStep.Pressure(time = 30, pressure = 9f)),
    )
}
