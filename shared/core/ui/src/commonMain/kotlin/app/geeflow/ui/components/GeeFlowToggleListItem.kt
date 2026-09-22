package app.geeflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowTheme
import kotlin.math.abs

@Composable
fun GeeFlowToggleListItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = 16.dp,
    ),
    modifier: Modifier = Modifier,
) {
    GeeFlowListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        contentPadding = contentPadding,
        trailingContent = {
            GeeFlowSwitch(
                checked = checked,
                onCheckedChange = onCheckedChanged,
                enabled = true,
            )
        },
    )
}

@Composable
fun GeeFlowToggleListItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    value: String,
    onOffClick: () -> Unit,
    onValueClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = 16.dp,
    ),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GeeFlowListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        contentPadding = contentPadding,
        trailingContent = {
            GeeFlowSwitch(
                checked = checked,
                value = value,
                onOffClick = onOffClick,
                onValueClick = onValueClick,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun GeeFlowToggleListItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    value: String,
    onCheckedChanged: (Boolean) -> Unit,
    onValueConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    items: List<String> = emptyList(),
    valueRange: ClosedFloatingPointRange<Float>? = null,
    unit: String = "",
    allowDecimal: Boolean? = null,
    inputTitle: String? = null,
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

    GeeFlowToggleListItem(
        title = title,
        subtitle = subtitle,
        checked = checked,
        value = value,
        onOffClick = { onCheckedChanged(false) },
        onValueClick = {
            if (enabled) {
                if (checked) {
                    showInputPad = true
                } else {
                    onCheckedChanged(true)
                }
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
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                inputTitle?.let {
                    GeeFlowDialogTopBar(title = it, onCloseClick = { showInputPad = false })
                    VerticalSpacer(16.dp)
                }
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
                        if (!checked) {
                            onCheckedChanged(true)
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
        GeeFlowToggleListItem(
            title = "Toggle List Item",
            subtitle = "Subtitle describing the toggle option.",
            checked = true,
            onCheckedChanged = {},
        )
        GeeFlowToggleListItem(
            title = "Brew Boiler",
            subtitle = "Current temperature: 93.0°",
            checked = true,
            value = "93.0°",
            items = (85..100).map { "$it.0" },
            unit = "°",
            onCheckedChanged = {},
            onValueConfirmed = {},
        )
    }
}
