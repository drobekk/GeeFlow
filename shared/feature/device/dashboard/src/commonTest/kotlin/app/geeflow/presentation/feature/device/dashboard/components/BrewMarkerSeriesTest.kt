package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BrewMarkerSeriesTest {
    @Test
    fun `when target and measurement timestamps differ then both values use the same marker time`() {
        val x = listOf(0.0, 0.73, 1.47, 2.11)
        val y = listOf(0.0, 4.0, 9.0, 9.0)
        val measured = BrewMarkerSeries(x, y, Color.Red, "bar")
        val targetX = (0..30).map { it / 10.0 }
        val target = BrewMarkerSeries(targetX, targetX.map { 7.0 }, Color.Blue, "bar")

        val actual = measured.valueAt(1.05)
        val planned = target.valueAt(1.05)

        assertNotNull(actual)
        assertEquals(4.0 + (1.05 - 0.73) / (1.47 - 0.73) * 5.0, actual, 0.000001)
        assertEquals(7.0, planned)
        assertEquals(listOf(0.0, 0.73, 1.47, 2.11), x)
        assertEquals(listOf(0.0, 4.0, 9.0, 9.0), y)
    }

    @Test
    fun `when marker is at a real reading then returns the exact value`() {
        val series = BrewMarkerSeries(listOf(0.0, 0.73, 1.47), listOf(0.0, 4.0, 9.0), Color.Red, "bar")

        val values = series.x.map { series.valueAt(it) }

        assertEquals(series.y, values)
    }

    @Test
    fun `when marker is outside measurements then does not invent a reading`() {
        val series = BrewMarkerSeries(listOf(1.0, 2.0), listOf(3.0, 9.0), Color.Red, "bar")

        val before = series.valueAt(0.9)
        val after = series.valueAt(2.1)
        val midpoint = series.valueAt(1.5)

        assertNull(before)
        assertNull(after)
        assertEquals(6.0, midpoint)
    }

    @Test
    fun `when series is empty or has one reading then handles missing intervals`() {
        val empty = BrewMarkerSeries(emptyList(), emptyList(), Color.Red, "bar")
        val single = BrewMarkerSeries(listOf(1.0), listOf(3.0), Color.Red, "bar")

        val missing = empty.valueAt(1.0)
        val exact = single.valueAt(1.0)
        val outside = single.valueAt(1.1)

        assertNull(missing)
        assertEquals(3.0, exact)
        assertNull(outside)
    }
}
