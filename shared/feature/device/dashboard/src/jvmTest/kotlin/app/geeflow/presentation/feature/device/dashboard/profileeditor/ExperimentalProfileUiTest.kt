@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.ExitCondition
import app.geeflow.data.brew.model.PhaseRamp
import app.geeflow.data.brew.model.RampStart
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.brew.model.ThresholdComparison
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.ui.theme.GeeFlowTheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

class ExperimentalProfileUiTest {
    private val step = ProfileEditorViewState.Step(1, StepType.Pressure, 15, 6f,
        ramp = PhaseRamp(RampStyle.EaseInOut, 3000, RampStart.PreviousTarget),
        exitConditions = listOf(ExitCondition(BrewMetric.PumpedVolume, ThresholdComparison.Above, 40f)),
        experimental = true)

    @Test fun portraitEditorShowsExperimentalPhaseAndConditionalPreview() = runSkikoComposeUiTest(size = Size(440f,
        900f)) {
        val phases = listOf(step.copy(id = 0, phaseId = "0", value = 3f, ramp = PhaseRamp()), step)
        setContent {
            GeeFlowTheme(false, lightColorScheme(), Modifier.fillMaxSize()) {
                ProfileEditorContent(ProfileEditorViewState(experimental = true, profileName = "Experimental",
                    pressureRange = 0f..12f, flowRange = 0f..8f, steps = phases,
                    targetData = BrewProgram.Phases(phases.map { it.toPhase() }).toTargetData()),
                    SnackbarHostState(), {})
            }
        }
        waitForIdle()
        onNodeWithText("Experimental · app control").assertExists()
        mainClock.advanceTimeBy(1000)
        saveImage(onRoot().captureToImage().toPixelMap(), "editor")
    }

    @Test fun transitionDialogShowsReadableResourceLabels() = runSkikoComposeUiTest(size = Size(600f, 1000f)) {
        setContent {
            GeeFlowTheme(false, lightColorScheme(), Modifier.fillMaxSize()) { AdvancedPhaseDialog(step, {}) }
        }
        waitForIdle()
        onNodeWithText("Ramp: Ease in-out").assertExists()
        onNodeWithText("Measurement: Water pumped in this phase (ml)").assertExists()
        saveImage(onAllNodes(isRoot())[1].captureToImage().toPixelMap(), "transition")
    }

    private fun saveImage(pixels: androidx.compose.ui.graphics.PixelMap, name: String) {
        val output = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) output.setRGB(x, y, pixels[x, y].toArgb())
        val file = File("build/reports/experimental/$name.png")
        file.parentFile.mkdirs()
        ImageIO.write(output, "png", file)
    }
}
