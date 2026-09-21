package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowTheme
import kotlin.math.abs

private const val DisabledAlpha = 0.38f
private val DefaultPillMinWidth = 80.dp

@Composable
fun GeeFlowValueListItem(
    title: String,
    subtitle: String,
    value: String,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = 16.dp,
    ),
) {
    val shape = MaterialTheme.shapes.large
    val primaryColor = MaterialTheme.colorScheme.primary

    GeeFlowListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        contentPadding = contentPadding,
        trailingContent = {
            Box(
                modifier = Modifier
                    .widthIn(min = DefaultPillMinWidth)
                    .clip(shape)
                    .background(primaryColor)
                    .border(2.dp, primaryColor, shape)
                    .clickable(enabled = enabled, onClick = onValueClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .alpha(if (enabled) 1f else DisabledAlpha),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@Composable
fun GeeFlowValueListItem(
    title: String,
    subtitle: String,
    value: String,
    onValueConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    items: List<String> = emptyList(),
    valueRange: ClosedFloatingPointRange<Float>? = null,
    unit: String = "",
    allowDecimal: Boolean? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = 16.dp,
    ),
) {
    var showInputPad by remember { mutableStateOf(false) }

    val effectiveRange = remember(items, valueRange) {
        if (valueRange != null) return@remember valueRange
        val numericValues = items.mapNotNull { it.extractFloat() }
        if (numericValues.isNotEmpty()) {
            numericValues.min()..numericValues.max()
        } else {
            0f..100f
        }
    }

    val effectiveAllowDecimal = remember(items, allowDecimal) {
        allowDecimal ?: items.any { it.contains('.') }
    }

    GeeFlowValueListItem(
        title = title,
        subtitle = subtitle,
        value = value,
        onValueClick = {
            if (enabled) {
                showInputPad = true
            }
        },
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
    )

    if (showInputPad) {
        GeeFlowDialog(
            onDismissRequest = { showInputPad = false },
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .wrapContentWidth(),
                contentAlignment = Alignment.Center,
            ) {
                GeeFlowInputPad(
                    valueRange = effectiveRange,
                    unit = unit,
                    allowDecimal = effectiveAllowDecimal,
                    onConfirm = { confirmedValue ->
                        val confirmedString = if (items.isNotEmpty()) {
                            items.firstOrNull { it.extractFloat() == confirmedValue }
                                ?: items.minByOrNull { abs((it.extractFloat() ?: Float.MAX_VALUE) - confirmedValue) }
                                ?: confirmedValue.toString()
                        } else {
                            if (effectiveAllowDecimal) {
                                confirmedValue.toString()
                            } else {
                                confirmedValue.toInt().toString()
                            }
                        }
                        onValueConfirmed(confirmedString)
                        showInputPad = false
                    },
                    onBack = { showInputPad = false },
                )
            }
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    Column {
        GeeFlowValueListItem(
            title = "Value List Item",
            subtitle = "Subtitle describing the value option.",
            value = "10 sec",
            onValueClick = {},
        )
    }
}
