package dev.drobek.geeflow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme

@Composable
fun GeeFlowToggleListItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GeeFlowListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
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
private fun PreviewLight() = GeeFlowTheme(false) {
    GeeFlowToggleListItem(
        title = "Toggle List Item",
        subtitle = "Subtitle describing the toggle option.",
        checked = true,
        onCheckedChanged = {},
    )
}

@Composable
@Preview
private fun PreviewDark() = GeeFlowTheme(true) {
    GeeFlowToggleListItem(
        title = "Toggle List Item",
        subtitle = "Subtitle describing the toggle option.",
        checked = false,
        onCheckedChanged = {},
    )
}
