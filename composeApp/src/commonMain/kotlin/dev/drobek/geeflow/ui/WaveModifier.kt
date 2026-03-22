package dev.drobek.geeflow.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

enum class WaveOrientation { Horizontal, Vertical }

fun Modifier.waveBackground(
    targetProgress: Float,
    color: Color,
    orientation: WaveOrientation = WaveOrientation.Horizontal,
    waves: Float = 1.2f,
    amplitude: Dp = 4.dp,
    durationMillis: Int = 3000,
    progressAnimationDurationMillis: Int = 1000,
    reversed: Boolean = false
): Modifier = composed {
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = progressAnimationDurationMillis),
        label = "WaveProgressAnimation"
    )

    if (progress <= 0f) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing)
        ),
        label = "phase"
    )

    val actualPhase = if (reversed) -phase else phase

    val amplitudeFactor by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMillis * 0.8).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitudeFactor"
    )

    this.drawBehind {
        val w = size.width
        val h = size.height

        if (progress >= 1f) {
            drawRect(color = color, size = size)
            return@drawBehind
        }

        val path = Path()
        val currentAmplitude = amplitude.toPx() * amplitudeFactor

        if (orientation == WaveOrientation.Horizontal) {
            // Horizontal wave: Fill bottom-to-top (default) or top-to-bottom (reversed)
            val fillLevel = if (reversed) {
                h * progress.coerceIn(0f, 1f)
            } else {
                h * (1f - progress.coerceIn(0f, 1f))
            }

            fun yAt(x: Float): Float = fillLevel + sin((2f * PI.toFloat()) * waves * (x / w) + actualPhase) * currentAmplitude

            if (reversed) {
                path.moveTo(0f, 0f)
                path.lineTo(0f, yAt(0f))
                var x = 0f
                while (x <= w) {
                    path.lineTo(x, yAt(x))
                    x += 4f
                }
                path.lineTo(w, yAt(w))
                path.lineTo(w, 0f)
            } else {
                path.moveTo(0f, h)
                path.lineTo(0f, yAt(0f))
                var x = 0f
                while (x <= w) {
                    path.lineTo(x, yAt(x))
                    x += 4f
                }
                path.lineTo(w, yAt(w))
                path.lineTo(w, h)
            }
            path.close()
        } else {
            // Vertical wave: Fill left-to-right (default) or right-to-left (reversed)
            val fillLevel = if (reversed) {
                w * (1f - progress.coerceIn(0f, 1f))
            } else {
                w * progress.coerceIn(0f, 1f)
            }

            fun xAt(y: Float): Float = fillLevel + sin((2f * PI.toFloat()) * waves * (y / h) + actualPhase) * currentAmplitude

            if (reversed) {
                path.moveTo(w, 0f)
                path.lineTo(xAt(0f), 0f)
                var y = 0f
                while (y <= h) {
                    path.lineTo(xAt(y), y)
                    y += 4f
                }
                path.lineTo(xAt(h), h)
                path.lineTo(w, h)
            } else {
                path.moveTo(0f, 0f)
                path.lineTo(xAt(0f), 0f)
                var y = 0f
                while (y <= h) {
                    path.lineTo(xAt(y), y)
                    y += 4f
                }
                path.lineTo(xAt(h), h)
                path.lineTo(0f, h)
            }
            path.close()
        }

        drawPath(path = path, color = color)
    }
}
