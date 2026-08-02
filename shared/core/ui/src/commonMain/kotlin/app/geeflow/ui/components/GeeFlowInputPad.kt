package app.geeflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.geeflow.ui.isHeightCompact
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_delete
import geeflow.shared.core.ui.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource

private const val DecimalSeparator = "."
private const val MaxDecimals = 1
private val DigitRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
)

/**
 * Numeric pad for typing a value in place of dragging a [GeeFlowSlider]. Starts empty so the value
 * is typed outright rather than edited, cannot confirm outside [valueRange], and drops the
 * separator key when [allowDecimal] is off. Meant to take over its host's whole content: delete
 * sits beside the value, and the bottom-right cell of the grid backs out until something is typed,
 * then confirms.
 */
@Composable
fun GeeFlowInputPad(
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onConfirm: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    allowDecimal: Boolean = true,
) {
    var input by remember { mutableStateOf("") }
    val value = input.toFloatOrNull()
    val valid = value != null && value in valueRange
    val displayColor = when {
        input.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
        valid -> color
        else -> MaterialTheme.colorScheme.error
    }

    val onKey: (String) -> Unit = { input = input.append(it, allowDecimal) }

    Column(
        // Sized to the key grid so the header's delete key lines up with the third column.
        modifier = modifier.width(GridWidth),
        verticalArrangement = Arrangement.spacedBy(KeySpacing),
    ) {
        InputHeader(
            input = input,
            unit = unit,
            color = displayColor,
            onDelete = { input = input.dropLast(1) },
        )

        DigitRows.forEach { row ->
            KeyRow { row.forEach { key -> PadKey(label = key, onClick = { onKey(key) }) } }
        }

        KeyRow {
            if (allowDecimal) {
                PadKey(label = DecimalSeparator, onClick = { onKey(DecimalSeparator) })
            } else {
                Spacer(Modifier.width(KeyWidth))
            }
            PadKey(label = "0", onClick = { onKey("0") })
            if (input.isEmpty()) {
                PadIconKey(
                    painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                    contentDescription = stringResource(Res.string.common_go_back),
                    onClick = onBack,
                )
            } else {
                ConfirmKey(
                    onClick = { value?.let(onConfirm) },
                    enabled = valid,
                    modifier = Modifier.size(width = KeyWidth, height = keyHeight()),
                )
            }
        }
    }
}

@Composable
private fun InputHeader(input: String, unit: String, color: Color, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(start = ValueInset),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = input.ifEmpty { "0" },
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                maxLines = 1,
            )
            HorizontalSpacer(4.dp)
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        HorizontalSpacer(KeySpacing)
        PadIconKey(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.Backspace),
            contentDescription = stringResource(Res.string.common_delete),
            onClick = onDelete,
        )
    }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) = Row(
    horizontalArrangement = Arrangement.spacedBy(KeySpacing),
    content = { content() },
)

@Composable
private fun PadKey(label: String, onClick: () -> Unit) = PadSurface(onClick) {
    Text(
        text = label,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PadIconKey(painter: Painter, contentDescription: String, onClick: () -> Unit) =
    PadSurface(onClick) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }

@Composable
private fun ConfirmKey(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Filled.Check),
            contentDescription = stringResource(Res.string.common_confirm),
        )
    }
}

@Composable
private fun PadSurface(onClick: () -> Unit, content: @Composable () -> Unit) = Surface(
    onClick = onClick,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.size(width = KeyWidth, height = keyHeight()),
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = { content() },
    )
}

private fun String.append(key: String, allowDecimal: Boolean): String = when {
    key == DecimalSeparator && (!allowDecimal || contains(DecimalSeparator)) -> this
    key == DecimalSeparator && isEmpty() -> "0$DecimalSeparator"
    contains(DecimalSeparator) && substringAfter(DecimalSeparator).length >= MaxDecimals -> this
    this == "0" && key != DecimalSeparator -> key
    else -> this + key
}

private val KeyWidth = 64.dp
private val ValueInset = 24.dp
private val KeySpacing = 8.dp
private val GridWidth = KeyWidth * 3 + KeySpacing * 2
private val CompactKeyHeight = 40.dp

/** Keys are round when there is room, and flatten into pills when the screen is too short for them. */
@Composable
private fun keyHeight(): Dp = if (isHeightCompact()) CompactKeyHeight else KeyWidth

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun PreviewDecimal() {
    GeeFlowInputPad(
        valueRange = 0f..12f,
        unit = "bar",
        onConfirm = {},
        onBack = {},
        color = MaterialTheme.colorScheme.error,
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun PreviewWhole() {
    GeeFlowInputPad(
        valueRange = 1f..500f,
        unit = "ml",
        onConfirm = {},
        onBack = {},
        allowDecimal = false,
    )
}
