@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.ui.theme.GeeFlowTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class RecordedBrewChartsTest {
    @Test
    fun portraitRecordedProfile() = render(Size(440f, 900f), "portrait")

    @Test
    fun landscapeRecordedProfile() = render(Size(960f, 440f), "landscape")

    private fun render(size: Size, name: String) = runSkikoComposeUiTest(size = size) {
        val recording = FreeHandRecording(
            FreeHandControlMode.Flow,
            List(77) { index ->
                val pressure = when {
                    index < 10 -> index * 0.9f
                    index < 40 -> 9f
                    else -> 9f - (index - 40) * 0.1f
                }
                FreeHandSample(index * 500L, BrewDataPoint(pressure, 0f, index * 3f, 6f, 0f))
            },
        )
        setContent {
            GeeFlowTheme(false, lightColorScheme(), Modifier.fillMaxSize()) {
                BrewCharts(
                    brew = Brew(),
                    visibleCharts = DashboardChartType.entries.toSet(),
                    targetData = recording.toTargetData(),
                )
            }
        }
        waitForIdle()
        mainClock.advanceTimeBy(1500)
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val output = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        var coloredPixels = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                output.setRGB(x, y, color.toArgb())
                if (kotlin.math.abs(color.red - color.blue) > 0.15f) coloredPixels++
            }
        }
        assertTrue(coloredPixels > 200, "The recorded reference curves must be visible before a brew starts")
        val file = File("build/reports/freehand-charts/$name.png")
        file.parentFile.mkdirs()
        ImageIO.write(output, "png", file)
    }
}
