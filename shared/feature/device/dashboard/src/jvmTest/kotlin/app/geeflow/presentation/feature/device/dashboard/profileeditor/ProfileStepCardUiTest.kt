@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import app.geeflow.ui.theme.GeeFlowTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

class ProfileStepCardUiTest {
    @Test
    fun narrowCardsShowDefaultNameRequiredTimeAndPassiveIndicators() = renderCards(
        style = StepCardStyle.Row,
        width = 300f,
    )

    @Test
    fun tilesShowDefaultNameRequiredTimeAndPassiveIndicators() = renderCards(
        style = StepCardStyle.Tile,
        width = 220f,
    )

    private fun renderCards(style: StepCardStyle, width: Float) = runSkikoComposeUiTest(
        size = Size(width = width, height = 280f),
    ) {
        val step = Step(
            id = 1,
            type = StepType.Pressure,
            timeSec = 60,
            value = 9f,
            experimental = true,
            ramp = PhaseRamp(style = RampStyle.Linear, durationMillis = 3000),
            exitConditions = listOf(
                BrewMetric.PhaseTime,
                BrewMetric.PumpedVolume,
                BrewMetric.CupWeight,
                BrewMetric.PumpPressure,
                BrewMetric.PumpFlow,
            ).map { metric ->
                ExitCondition(metric = metric, comparison = ThresholdComparison.Above, threshold = 20f)
            },
        )
        setContent {
            GeeFlowTheme(darkMode = false, colorScheme = lightColorScheme()) {
                Column {
                    StepCard(
                        step = step,
                        number = 2,
                        style = style,
                        isDragging = false,
                        onEvent = {},
                        modifier = Modifier.height(132.dp),
                    )
                    StepCard(
                        step = step.copy(phaseName = "Long custom extraction phase name", experimental = false),
                        number = 3,
                        style = style,
                        isDragging = false,
                        onEvent = {},
                        modifier = Modifier.height(132.dp),
                    )
                }
            }
        }
        onNodeWithText(text = "Step 2", useUnmergedTree = true).assertExists()
        onNodeWithText(text = "60 sec", useUnmergedTree = true).assertDoesNotExist()
        onAllNodesWithText(text = "20 sec", useUnmergedTree = true).assertCountEquals(2)
        listOf("Time (Required)", "Ramp", "Total volume", "Total weight", "Pump pressure", "Pump flow").forEach { label ->
            onAllNodesWithContentDescription(label = label, useUnmergedTree = true).assertCountEquals(2)
        }
        onNodeWithContentDescription(
            label = "Experimental",
            useUnmergedTree = true,
        ).assertHasNoClickAction()
        val pixels = onRoot().captureToImage().toPixelMap()
        val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                image.setRGB(x, y, pixels[x, y].toArgb())
            }
        }
        val output = File("build/reports/experimental/step-cards-${style.name.lowercase()}.png")
        output.parentFile.mkdirs()
        ImageIO.write(image, "png", output)
    }
}
