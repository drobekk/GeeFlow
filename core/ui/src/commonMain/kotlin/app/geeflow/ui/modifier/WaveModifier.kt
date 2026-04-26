package app.geeflow.ui.modifier

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

fun Modifier.waveBackground(
    targetProgress: Float,
    color: Color,
    orientation: WaveOrientation = WaveOrientation.Horizontal,
    waves: Float = 1.2f,
    amplitude: Dp = 4.dp,
    durationMillis: Int = 3000,
    progressAnimationDurationMillis: Int = 1000,
    reversed: Boolean = false,
): Modifier = composed {
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = if (targetProgress == 0f) 300 else progressAnimationDurationMillis,
            easing = LinearEasing,
        ),
        label = "WaveProgressAnimation",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
        ),
        label = "phase",
    )

    val actualPhase = if (reversed) -phase else phase

    val amplitudeFactor by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMillis * 0.8).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "amplitudeFactor",
    )

    this.drawBehind {
        if (progress <= 0f) return@drawBehind

        val w = size.width
        val h = size.height

        if (progress >= 1f) {
            drawRect(color = color, size = size)
            return@drawBehind
        }

        val currentAmplitude = amplitude.toPx() * amplitudeFactor
        val path = if (orientation == WaveOrientation.Horizontal) {
            buildHorizontalWavePath(w, h, progress, waves, actualPhase, currentAmplitude, reversed)
        } else {
            buildVerticalWavePath(w, h, progress, waves, actualPhase, currentAmplitude, reversed)
        }

        drawPath(path = path, color = color)
    }
}

private fun buildHorizontalWavePath(
    w: Float,
    h: Float,
    progress: Float,
    waves: Float,
    phase: Float,
    amplitude: Float,
    reversed: Boolean,
): Path {
    val fillLevel = if (reversed) h * progress.coerceIn(0f, 1f) else h * (1f - progress.coerceIn(0f, 1f))
    fun yAt(x: Float) = fillLevel + sin((2f * PI.toFloat()) * waves * (x / w) + phase) * amplitude

    return Path().apply {
        if (reversed) {
            moveTo(0f, 0f)
            lineTo(0f, yAt(0f))
            var x = 0f
            while (x <= w) {
                lineTo(x, yAt(x))
                x += WaveSampleStepPx
            }
            lineTo(w, yAt(w))
            lineTo(w, 0f)
        } else {
            moveTo(0f, h)
            lineTo(0f, yAt(0f))
            var x = 0f
            while (x <= w) {
                lineTo(x, yAt(x))
                x += WaveSampleStepPx
            }
            lineTo(w, yAt(w))
            lineTo(w, h)
        }
        close()
    }
}

private fun buildVerticalWavePath(
    w: Float,
    h: Float,
    progress: Float,
    waves: Float,
    phase: Float,
    amplitude: Float,
    reversed: Boolean,
): Path {
    val fillLevel = if (reversed) w * (1f - progress.coerceIn(0f, 1f)) else w * progress.coerceIn(0f, 1f)
    fun xAt(y: Float) = fillLevel + sin((2f * PI.toFloat()) * waves * (y / h) + phase) * amplitude

    return Path().apply {
        if (reversed) {
            moveTo(w, 0f)
            lineTo(xAt(0f), 0f)
            var y = 0f
            while (y <= h) {
                lineTo(xAt(y), y)
                y += WaveSampleStepPx
            }
            lineTo(xAt(h), h)
            lineTo(w, h)
        } else {
            moveTo(0f, 0f)
            lineTo(xAt(0f), 0f)
            var y = 0f
            while (y <= h) {
                lineTo(xAt(y), y)
                y += WaveSampleStepPx
            }
            lineTo(xAt(h), h)
            lineTo(0f, h)
        }
        close()
    }
}

enum class WaveOrientation {
    Horizontal,
    Vertical
}

private const val WaveSampleStepPx = 4f
