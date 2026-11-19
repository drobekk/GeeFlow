package app.geeflow.domain.device

import app.geeflow.data.device.BookooScalePacketEvent
import app.geeflow.data.device.model.BookooScaleFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class ScaleTelemetryTest {
    @Test
    fun recordsFullScaleFieldsAndDerivesFlowFromWeightSamples() {
        val startedAt = Clock.System.now()
        val first = BookooScalePacketEvent(
            receivedAt = startedAt + 100.milliseconds,
            packetHex = "030B0000012C",
            decoded = true,
            frame = BookooScaleFrame(300, 2.35f, 0.78f, 91),
        ).toBrewScaleSample(startedAt, null)
        val second = BookooScalePacketEvent(
            receivedAt = startedAt + 300.milliseconds,
            packetHex = "030B00000190",
            decoded = true,
            frame = BookooScaleFrame(500, 2.51f, 0.80f, 90),
        ).toBrewScaleSample(startedAt, first)

        assertEquals(100L, first.elapsedMillis)
        assertEquals(2.35f, first.weightGrams)
        assertEquals(0.78f, first.reportedFlowGramsPerSecond)
        assertEquals(91, first.batteryPercent)
        assertEquals("030B0000012C", first.rawPacketHex)
        assertTrue(kotlin.math.abs(requireNotNull(second.derivedFlowGramsPerSecond) - 0.8f) < 0.001f)
    }

    @Test
    fun retainsUndecodedPacketsWithoutInventingMeasurements() {
        val startedAt = Clock.System.now()
        val sample = BookooScalePacketEvent(
            startedAt,
            "010203",
            decoded = false,
        ).toBrewScaleSample(startedAt, null)

        assertEquals("010203", sample.rawPacketHex)
        assertNull(sample.weightGrams)
        assertNull(sample.reportedFlowGramsPerSecond)
        assertNull(sample.derivedFlowGramsPerSecond)
    }
}
