package app.geeflow.presentation.feature.device.dashboard.freecontrol

import kotlin.test.Test
import kotlin.test.assertEquals

class ManualTargetTest {
    @Test
    fun `repeated tenth steps retain the displayed control target`() {
        var value = 6f
        repeat(30) { value = steppedManualTarget(value, 0.1f, 0f..12f) }
        assertEquals(9f, value)
        assertEquals(90, (value * 10).toInt())
        repeat(30) { value = steppedManualTarget(value, -0.1f, 0f..12f) }
        assertEquals(6f, value)
        assertEquals(60, (value * 10).toInt())
    }

    @Test
    fun `steps stay inside device limits`() {
        assertEquals(12f, steppedManualTarget(12f, 0.1f, 0f..12f))
        assertEquals(0f, steppedManualTarget(0f, -0.1f, 0f..12f))
    }

    @Test
    fun `relative pickup never jumps and fine motion gives quarter sensitivity`() {
        assertEquals(6f, draggedManualTarget(6f, 0f, 400f, 0f..12f, false))
        assertEquals(9f, draggedManualTarget(6f, -100f, 400f, 0f..12f, false))
        assertEquals(6.75f, draggedManualTarget(6f, -100f, 400f, 0f..12f, true))
        assertEquals(3f, draggedManualTarget(6f, 100f, 400f, 0f..12f, false))
    }

    @Test
    fun `gesture clamps at both limits and handles a zero sized pad`() {
        assertEquals(12f, draggedManualTarget(11f, -1000f, 400f, 0f..12f, false))
        assertEquals(0f, draggedManualTarget(1f, 1000f, 400f, 0f..12f, false))
        assertEquals(6f, draggedManualTarget(6f, -1000f, 0f, 0f..12f, false))
    }
}
