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
import app.geeflow.presentation.feature.device.dashboard.main.RecipeDashboardReviewPreviewContent
import app.geeflow.ui.theme.ClearScreenTheme
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.colorscheme.EspressoSeed
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val directory = File(args.singleOrNull() ?: "docs/screenshots/review-v02")
    directory.mkdirs()
    render(PhoneWidthPx, PhoneHeightPx, PhoneDensity, PhoneStatusBarPx, File(directory, "s24-plus-active-brew.png"))
    render(TabletWidthPx, TabletHeightPx, TabletDensity, TabletStatusBarPx, File(directory, "tablet-active-brew.png"))
    render(PhoneWidthPx, PhoneHeightPx, PhoneDensity, PhoneStatusBarPx, File(directory, "s24-plus-recipe-browser.png"), initialPage = 0)
    render(TabletWidthPx, TabletHeightPx, TabletDensity, TabletStatusBarPx, File(directory, "tablet-recipe-browser.png"), initialPage = 0)
    exitProcess(0)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun render(width: Int, height: Int, density: Float, statusBarHeight: Int, output: File, initialPage: Int = 1) {
    ImageComposeScene(width, height, Density(density)) {
        GeeFlowTheme(
            darkMode = false,
            colorScheme = rememberDynamicColorScheme(
                seedColor = EspressoSeed,
                isDark = false,
                style = PaletteStyle.TonalSpot,
            ),
        ) {
            ClearScreenTheme {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = (statusBarHeight / density).dp),
                    content = { RecipeDashboardReviewPreviewContent(initialPage) },
                )
            }
        }
    }.use { scene ->
        scene.render()
        Thread.sleep(600L)
        scene.render(0L)
        val encoded = scene.render(1_200_000_000L)
            .encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("Could not encode ${output.name}")
        val rendered = ImageIO.read(encoded.inputStream()) ?: error("Could not decode ${output.name}")
        check(ImageIO.write(rendered, "png", output)) { "Could not write ${output.absolutePath}" }
    }
    println("Generated ${output.absolutePath}")
}

private const val PhoneWidthPx = 1440
private const val PhoneHeightPx = 3120
private const val PhoneDensity = 3.5f
private const val PhoneStatusBarPx = 90
private const val TabletWidthPx = 1280
private const val TabletHeightPx = 800
private const val TabletDensity = 1f
private const val TabletStatusBarPx = 24
