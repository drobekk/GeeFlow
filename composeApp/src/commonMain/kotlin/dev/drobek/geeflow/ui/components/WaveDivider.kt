package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.WaveOrientation
import dev.drobek.geeflow.ui.waveBackground

@Composable
fun WaveDivider(
    color: Color,
    modifier: Modifier = Modifier,
    thickness: Dp = 20.dp,
    waves: Float = 1.5f,
    durationMillis: Int = 4000,
    orientation: WaveOrientation = WaveOrientation.Horizontal,
    reversed: Boolean = false
) {
    Box(
        modifier = modifier
            .then(
                when (orientation) {
                    WaveOrientation.Horizontal -> Modifier.fillMaxWidth().height(thickness)
                    WaveOrientation.Vertical -> Modifier.fillMaxHeight().width(thickness)
                }
            )
            .waveBackground(
                targetProgress = 0.5f,
                color = color,
                orientation = orientation,
                waves = waves,
                durationMillis = durationMillis,
                reversed = reversed
            )
    )
}
