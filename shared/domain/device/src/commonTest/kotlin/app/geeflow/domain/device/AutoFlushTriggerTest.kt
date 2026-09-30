package app.geeflow.domain.device

import app.geeflow.data.device.model.DeviceState.BrewStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoFlushTriggerTest {
    @Test
    fun `completed shot triggers once but automatic manual brew does not trigger again`() {
        val trigger = AutoFlushTrigger()
        assertFalse(trigger.onState(BrewStatus.Idle, connected = true))
        assertFalse(trigger.onState(BrewStatus.Profile, connected = true))
        assertTrue(trigger.onState(BrewStatus.Idle, connected = true))
        assertFalse(trigger.onState(BrewStatus.Idle, connected = true))

        trigger.automaticStartRequested()
        assertFalse(trigger.onState(BrewStatus.Manual, connected = true))
        assertFalse(trigger.onState(BrewStatus.Idle, connected = true))

        assertFalse(trigger.onState(BrewStatus.Manual, connected = true))
        assertTrue(trigger.onState(BrewStatus.Idle, connected = true))
    }

    @Test
    fun `disconnection and cleaning do not trigger auto flush`() {
        val trigger = AutoFlushTrigger()
        trigger.onState(BrewStatus.Profile, connected = true)
        assertFalse(trigger.onState(BrewStatus.Idle, connected = false))
        assertFalse(trigger.onState(BrewStatus.Idle, connected = true))
        trigger.onState(BrewStatus.Cleaning, connected = true)
        assertFalse(trigger.onState(BrewStatus.Idle, connected = true))
    }

    @Test
    fun `failed automatic start does not suppress a later manual brew`() {
        val trigger = AutoFlushTrigger()
        trigger.automaticStartRequested()
        trigger.automaticStartFailed()
        assertFalse(trigger.onState(BrewStatus.Manual, connected = true))
        assertTrue(trigger.onState(BrewStatus.Idle, connected = true))
    }

    @Test
    fun `changing operation after automatic flush does not suppress a later shot`() {
        val trigger = AutoFlushTrigger()
        trigger.automaticStartRequested()
        assertFalse(trigger.onState(BrewStatus.Manual, connected = true))
        assertFalse(trigger.onState(BrewStatus.Cleaning, connected = true))
        assertFalse(trigger.onState(BrewStatus.Idle, connected = true))
        assertFalse(trigger.onState(BrewStatus.Profile, connected = true))
        assertTrue(trigger.onState(BrewStatus.Idle, connected = true))
    }
}
