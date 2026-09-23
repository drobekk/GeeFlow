package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.ui.modifier.WaveOrientation
import app.geeflow.ui.modifier.waveBackground
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun GeeFlowSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    vertical: Boolean = false,
    allowDecimal: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val fillColor = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val trackColor = if (enabled) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val valueStyle = MaterialTheme.typography.titleLarge
    val unitStyle = MaterialTheme.typography.labelSmall
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val valueLabel = value.formatSliderValue(allowDecimal)

    val updatedOnValueChange by rememberUpdatedState(onValueChange)

    Box(
        modifier = modifier
            .clip(shape)
            .background(trackColor)
            .then(
                if (enabled) {
                    Modifier.waveBackground(
                        progress = fraction,
                        color = fillColor,
                        amplitude = (1.2).dp,
                        orientation = if (vertical) WaveOrientation.Horizontal else WaveOrientation.Vertical,
                    )
                } else {
                    Modifier.drawBehind {
                        if (vertical) {
                            val fillHeight = size.height * fraction
                            drawRect(
                                color = fillColor,
                                topLeft = Offset(0f, size.height - fillHeight),
                                size = Size(size.width, fillHeight),
                            )
                        } else {
                            drawRect(
                                color = fillColor,
                                size = Size(size.width * fraction, size.height),
                            )
                        }
                    }
                },
            )
            .then(
                if (enabled) {
                    Modifier
                        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                        .pointerInput(valueRange, vertical) {
                            detectDragGestures { change, _ ->
                                val newFraction = if (vertical) {
                                    1f - change.position.y / size.height
                                } else {
                                    change.position.x / size.width
                                }
                                updatedOnValueChange(
                                    valueRange.start +
                                        newFraction.coerceIn(0f, 1f) * (valueRange.endInclusive - valueRange.start),
                                )
                            }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        if (vertical) {
            Text(
                text = unit,
                style = unitStyle,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
            Text(
                text = valueLabel,
                style = valueStyle,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
        } else {
            Text(
                text = valueLabel,
                style = valueStyle,
                color = textColor,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 20.dp),
            )
            Text(
                text = unit,
                style = unitStyle,
                color = textColor,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 20.dp),
            )
        }
    }
}

private fun Float.formatSliderValue(allowDecimal: Boolean): String {
    if (!allowDecimal) return roundToInt().toString()
    val factor = 10f.pow(1)
    return (round(this * factor) / factor).toString()
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun PreviewHorizontal() {
    GeeFlowSlider(
        value = 6.5f,
        onValueChange = {},
        valueRange = 0f..12f,
        unit = "bar",
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun PreviewVertical() {
    GeeFlowSlider(
        value = 3.5f,
        onValueChange = {},
        valueRange = 0f..8f,
        unit = "ml/s",
        color = Color(0xFF3767D5),
        vertical = true,
        modifier = Modifier
            .width(64.dp)
            .height(200.dp),
    )
}
