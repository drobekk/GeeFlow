package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.GeeFlowThemePreview

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
@Preview
private fun PreviewLight() = GeeFlowThemePreview(false) {
    GeeFlowToggleListItem(
        title = "Toggle List Item",
        subtitle = "Subtitle describing the toggle option.",
        checked = true,
        onCheckedChanged = {},
    )
}

@Composable
@Preview
private fun PreviewDark() = GeeFlowThemePreview(true) {
    GeeFlowToggleListItem(
        title = "Toggle List Item",
        subtitle = "Subtitle describing the toggle option.",
        checked = false,
        onCheckedChanged = {},
    )
}
