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
    orientation: WaveOrientation = WaveOrientation.Horizontal,
    reverseFill: Boolean = false
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
        modifier = modifier.then(
            when (orientation) {
                WaveOrientation.Horizontal -> Modifier.fillMaxWidth().height(thickness)
                WaveOrientation.Vertical -> Modifier.fillMaxHeight().width(thickness)
            }
        )
    ) {
        val w = size.width
        val h = size.height
        val path = Path()
        val waveRangePx = thickness.toPx()

        if (orientation == WaveOrientation.Horizontal) {
            val midY = h / 2f
            val amplitude = (waveRangePx / 2f) * amplitudeFactor
            val fillEdge = if (reverseFill) 0f else h

            fun yAt(x: Float): Float = midY + sin((2f * PI.toFloat()) * waves * (x / w) + phase) * amplitude

            path.moveTo(0f, fillEdge)
            path.lineTo(0f, yAt(0f))

            var x = 0f
            while (x <= w) {
                path.lineTo(x, yAt(x))
                x += 2f
            }

            path.lineTo(w, yAt(w))
            path.lineTo(w, fillEdge)
            path.close()

        } else {
            val midX = w / 2f
            val amplitude = (waveRangePx / 2f) * amplitudeFactor
            val fillEdge = if (reverseFill) 0f else w

            fun xAt(y: Float): Float = midX + sin((2f * PI.toFloat()) * waves * (y / h) + phase) * amplitude

            path.moveTo(fillEdge, 0f)
            path.lineTo(xAt(0f), 0f)

            var y = 0f
            while (y <= h) {
                path.lineTo(xAt(y), y)
                y += 2f
            }

            path.lineTo(xAt(h), h)
            path.lineTo(fillEdge, h)
            path.close()
        }

        drawPath(path = path, color = color)
    }
}
