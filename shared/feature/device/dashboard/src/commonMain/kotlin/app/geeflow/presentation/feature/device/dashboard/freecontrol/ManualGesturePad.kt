package app.geeflow.presentation.feature.device.dashboard.freecontrol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import kotlin.math.roundToInt

@Composable
internal fun ManualGesturePad(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    fine: Boolean,
    enabled: Boolean,
    color: Color,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValue)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val padDescription = clearText(ClearText.PadDescription, unit)
    val usable = enabled && range.endInclusive > range.start
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = PadTint))
            .semantics {
                contentDescription = padDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(range), range)
                if (!usable) disabled()
                setProgress { requested ->
                    if (usable) change(steppedManualTarget(requested, 0f, range))
                    usable
                }
            }
            .pointerInput(range, fine, usable) {
                var dragValue = current
                detectDragGestures(
                    onDragStart = { dragValue = current },
                    onDrag = { pointer, delta ->
                        if (usable) {
                            pointer.consume()
                            dragValue = draggedManualTarget(dragValue, delta.y, size.height.toFloat(), range, fine)
                            change(steppedManualTarget(dragValue, 0f, range))
                        }
                    },
                )
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 52.dp)) {
                val span = range.endInclusive - range.start
                val fraction = if (span > 0f) ((value - range.start) / span).coerceIn(0f, 1f) else 0f
                val y = size.height * (1f - fraction)
                val rail = 16.dp.toPx()
                drawRoundRect(outline, Offset(size.width / 2 - rail / 2, 0f), Size(rail, size.height), CornerRadius(rail))
                drawRoundRect(color, Offset(size.width / 2 - rail / 2, y), Size(rail, size.height - y), CornerRadius(rail))
                repeat(TickCount + 1) { index ->
                    val tickY = size.height * index / TickCount
                    drawLine(outline, Offset(0f, tickY), Offset(size.width, tickY), 1.dp.toPx())
                }
                drawRoundRect(
                    color,
                    Offset(0f, y - 14.dp.toPx()),
                    Size(size.width, 28.dp.toPx()),
                    CornerRadius(14.dp.toPx()),
                )
                drawLine(Color.White, Offset(size.width / 2 - rail, y), Offset(size.width / 2 + rail, y), 3.dp.toPx())
            }
            (1 until TickCount).forEach { index ->
                val fraction = index.toFloat() / TickCount
                val mark = range.endInclusive - (range.endInclusive - range.start) * fraction
                Text(
                    text = mark.formatScaleLabel(),
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(y = maxHeight * fraction - 8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                        .padding(end = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
            }
        }
        Text(
            clearText(ClearText.PadIncrease, ((range.endInclusive * Precision).roundToInt() / Precision).toString(), unit),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            clearText(ClearText.PadDecrease, range.start.toString(), unit),
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

private const val PadTint = 0.06f
private const val TickCount = 6
private const val Precision = 10f

private fun Float.formatScaleLabel(): String {
    val tenths = (this * Precision).roundToInt()
    return if (tenths % Precision.toInt() == 0) {
        (tenths / Precision.toInt()).toString()
    } else {
        "${tenths / Precision.toInt()}.${kotlin.math.abs(tenths % Precision.toInt())}"
    }
}
