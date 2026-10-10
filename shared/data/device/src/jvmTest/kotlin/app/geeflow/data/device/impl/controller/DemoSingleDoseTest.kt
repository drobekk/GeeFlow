package app.geeflow.data.device.impl.controller

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DemoSingleDoseTest {
    @Test
    fun `when demo search finishes then two grinders are available and scale is unchanged`() = runTest {
        val controller = DemoDeviceController(backgroundScope)
        val scale = controller.deviceState.value.smartScale

        controller.setSingleDoseGrinderConnectivity(true)
        advanceTimeBy(10_001)
        runCurrent()

        assertEquals(listOf("Milo Demo", "BGM_Demo"), controller.foundSingleDoseGrinders.value.map { it.name })
        assertFalse(controller.deviceState.value.singleDoseGrinderSearchActive)
        assertEquals(scale, controller.deviceState.value.smartScale)
    }

    @Test
    fun `when selecting another demo grinder then only the new grinder is connected`() = runTest {
        val controller = DemoDeviceController(backgroundScope)
        controller.setSingleDoseGrinderConnectivity(true)
        advanceTimeBy(2_001)
        runCurrent()
        controller.connectSingleDoseGrinder("Milo Demo")

        controller.connectSingleDoseGrinder("BGM_Demo")

        assertEquals("BGM_Demo", controller.deviceState.value.singleDoseGrinder?.name)
        assertEquals(
            listOf("BGM_Demo"),
            controller.foundSingleDoseGrinders.value.filter { it.isConnected }.map { it.name },
        )
    }

    @Test
    fun `when disabling during demo connection then delayed connection cannot restore grinder`() = runTest {
        val controller = DemoDeviceController(backgroundScope)
        controller.setSingleDoseGrinderConnectivity(true)
        val connection = async { controller.connectSingleDoseGrinder("Milo Demo") }
        runCurrent()

        controller.setSingleDoseGrinderConnectivity(false)
        connection.await()
        advanceTimeBy(12_000)
        runCurrent()

        assertNull(controller.deviceState.value.singleDoseGrinder)
        assertFalse(controller.deviceState.value.singleDoseGrinderEnabled)
        assertFalse(controller.deviceState.value.singleDoseGrinderSearchActive)
        assertTrue(controller.foundSingleDoseGrinders.value.isEmpty())
    }

    @Test
    fun `when machine disconnects during search then delayed results cannot restore grinder list`() = runTest {
        val controller = DemoDeviceController(backgroundScope)
        controller.setSingleDoseGrinderConnectivity(true)
        runCurrent()

        controller.disconnect()
        advanceTimeBy(12_000)
        runCurrent()

        assertFalse(controller.deviceState.value.singleDoseGrinderEnabled)
        assertTrue(controller.foundSingleDoseGrinders.value.isEmpty())
    }

    @Test
    fun `when disconnecting demo grinder then it stays available for reconnecting`() = runTest {
        val controller = DemoDeviceController(backgroundScope)
        controller.setSingleDoseGrinderConnectivity(true)
        advanceTimeBy(2_001)
        runCurrent()
        controller.connectSingleDoseGrinder("Milo Demo")

        controller.disconnectSingleDoseGrinder()

        assertNull(controller.deviceState.value.singleDoseGrinder)
        assertEquals(2, controller.foundSingleDoseGrinders.value.size)
        assertTrue(controller.foundSingleDoseGrinders.value.none { it.isConnected })
    }
}
