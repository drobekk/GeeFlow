package app.geeflow.data.brew.impl

import app.geeflow.data.brew.model.BrewScaleSample
import kotlin.test.Test
import kotlin.test.assertEquals

class BrewScaleSamplesAdapterTest {
    @Test
    fun scaleSamplesRoundTripThroughHistoryJsonAdapter() {
        val samples = listOf(
            BrewScaleSample(
                elapsedMillis = 187,
                receivedAtEpochMillis = 1_800_000_000_187,
                scaleElapsedMillis = 720,
                weightGrams = 2.37f,
                reportedFlowGramsPerSecond = 1.14f,
                derivedFlowGramsPerSecond = 1.08f,
                batteryPercent = 83,
                rawPacketHex = "030B000002D0",
                decoded = true,
            ),
            BrewScaleSample(188, 1_800_000_000_188, rawPacketHex = "010203", decoded = false),
        )

        assertEquals(samples, BrewScaleSamplesAdapter.decode(BrewScaleSamplesAdapter.encode(samples)))
    }
}
