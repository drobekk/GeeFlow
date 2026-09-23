package app.geeflow.presentation.feature.device.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardPreviewContent
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorPreviewContent
import app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorPreviewContent
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.colorscheme.EspressoSeed
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val directory = File(args.singleOrNull() ?: "docs/screenshots")
    directory.mkdirs()

    val overlay = ImageIO.read(File(directory, "phone-overlay.png")) ?: error("Missing phone-overlay.png")
    require(overlay.width == PhoneWidth && overlay.height == PhoneHeight)

    val screens = listOf<@Composable () -> Unit>(
        { DeviceDashboardPreviewContent() },
        { ProfileEditorPreviewContent() },
        { StepEditorPreviewContent() },
    )

    screens.forEachIndexed { index, screen ->
        render(screen, TabletWidth, TabletHeight, 0, File(directory, "tablet-${index + 1}.png"))
        render(screen, PhoneWidth, PhoneHeight, PhoneStatusBarHeight, File(directory, "phone-${index + 1}.png"), overlay)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun render(
    screen: @Composable () -> Unit,
    width: Int,
    height: Int,
    statusBarHeight: Int,
    output: File,
    overlay: BufferedImage? = null,
) {
    val density = if (width == PhoneWidth) PhoneDensity else TabletDensity

    ImageComposeScene(width, height, Density(density)) {
        GeeFlowTheme(
            darkMode = false,
            colorScheme = rememberDynamicColorScheme(
                seedColor = EspressoSeed,
                isDark = false,
                style = PaletteStyle.TonalSpot,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = (statusBarHeight / density).dp),
                content = { screen() },
            )
        }
    }.use { scene ->
        scene.render()
        // Let Vico publish its chart model, then advance the frame past the chart animation.
        Thread.sleep(ChartDataWaitMillis)
        scene.render(0L)
        val encoded = scene.render(ScreenshotFrameMillis * NanosPerMillisecond)
            .encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("Could not encode ${output.name}")
        val rendered = ImageIO.read(encoded.inputStream()) ?: error("Could not decode ${output.name}")

        if (overlay == null) {
            ImageIO.write(rendered, "png", output)
        } else {
            val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = result.createGraphics()
            try {
                graphics.drawImage(rendered, 0, 0, null)
                graphics.drawImage(overlay, 0, 0, null)
            } finally {
                graphics.dispose()
            }
            ImageIO.write(result, "png", output)
        }
    }
    println("Generated ${output.absolutePath}")
}

private const val TabletWidth = 2752
private const val TabletHeight = 2064
private const val PhoneWidth = 1284
private const val PhoneHeight = 2778
private const val PhoneStatusBarHeight = 151
private const val TabletDensity = 3.0f
private const val PhoneDensity = 3.2f
private const val ChartDataWaitMillis = 600L
private const val ScreenshotFrameMillis = 1_200L
private const val NanosPerMillisecond = 1_000_000L
