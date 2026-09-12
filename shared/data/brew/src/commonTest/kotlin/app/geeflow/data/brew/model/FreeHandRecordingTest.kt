package app.geeflow.data.brew.model

import app.geeflow.data.brew.impl.FreeHandRecordingAdapter
import kotlin.test.Test
import kotlin.test.assertEquals

class FreeHandRecordingTest {
    private fun sample(time: Long, pressure: Float) = FreeHandSample(time, BrewDataPoint(pressure, 0f, 20f, 4f, 0f))

    @Test
    fun resamplesAllOfLongShotAndPreservesRawOvershoot() {
        val source = FreeHandRecording(FreeHandControlMode.Flow, List(77) { sample(it * 500L, 12.9f) })
        assertEquals(77, source.playbackPoints().size)
        assertEquals(12.9f, source.playbackPoints().last().pressure)
        assertEquals(12.9f, source.samples.last().data.pressure)
        assertEquals(source, FreeHandRecordingAdapter.decode(FreeHandRecordingAdapter.encode(source)))
    }

    @Test
    fun retainsLongRecordingsWithoutTransportLimits() {
        val source = FreeHandRecording(FreeHandControlMode.Pressure, List(128) { sample(it * 500L, 6f) })
        assertEquals(128, source.playbackPoints().size)
        assertEquals(127, source.copy(samples = source.samples.take(127)).playbackPoints().size)
    }

    @Test
    fun irregularTelemetryUsesNearestActualMeasurement() {
        val source =
            FreeHandRecording(FreeHandControlMode.Pressure, listOf(sample(0, 1f), sample(510, 2f), sample(990, 3f)))
        assertEquals(listOf(1f, 2f, 3f), source.playbackPoints().map { it.pressure })
    }

    @Test
    fun retainsFinalMeasurementWhenTelemetryArrivesFasterThanPlayback() {
        val source = FreeHandRecording(
            FreeHandControlMode.Flow,
            listOf(sample(0, 1f), sample(500, 2f), sample(600, 3f)),
        )
        assertEquals(listOf(1f, 3f), source.playbackPoints().map { it.pressure })
    }
}
