package dev.drobek.geeflow.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

enum class WaveOrientation { Horizontal, Vertical }

@Composable
fun WaveDivider(
    color: Color,
    modifier: Modifier = Modifier,
    thickness: Dp = 20.dp,
    waves: Float = 1.5f,
    durationMillis: Int = 4000,
    orientation: WaveOrientation = WaveOrientation.Horizontal
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing)
        ),
        label = "phase"
    )

    val amplitudeFactor by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitudeFactor"
    )

    Canvas(
        modifier = when (orientation) {
            WaveOrientation.Horizontal -> modifier
                .fillMaxWidth()
                .height(thickness)

            WaveOrientation.Vertical -> modifier
                .fillMaxHeight()
                .width(thickness)
        }
    ) {
        val w = size.width
        val h = size.height
        val step = 2f
        val path = Path()
        val borderPath = Path()

        if (orientation == WaveOrientation.Horizontal) {

            val midY = h / 2f
            val amplitude = (h / 2f) * amplitudeFactor

            fun yAt(x: Float): Float {
                val t = x / w
                val angle = (2f * PI.toFloat()) * waves * t + phase
                return midY + sin(angle) * amplitude
            }

            path.moveTo(0f, yAt(0f))
            borderPath.moveTo(0f, yAt(0f))

            var x = 0f
            while (x <= w + step) {
                val xx = x.coerceAtMost(w)
                val yy = yAt(xx)
                path.lineTo(xx, yy)
                borderPath.lineTo(xx, yy)
                x += step
            }

            path.lineTo(w, 0f)
            path.lineTo(0f, 0f)
            path.close()

        } else {

            val midX = w / 2f
            val amplitude = (w / 2f) * amplitudeFactor

            fun xAt(y: Float): Float {
                val t = y / h
                val angle = (2f * PI.toFloat()) * waves * t + phase
                return midX + sin(angle) * amplitude
            }

            path.moveTo(xAt(0f), 0f)
            borderPath.moveTo(xAt(0f), 0f)

            var y = 0f
            while (y <= h + step) {
                val yy = y.coerceAtMost(h)
                val xx = xAt(yy)
                path.lineTo(xx, yy)
                borderPath.lineTo(xx, yy)
                y += step
            }

            path.lineTo(0f, h)
            path.lineTo(0f, 0f)
            path.close()
        }

        drawPath(path = path, color = color)
    }
}
