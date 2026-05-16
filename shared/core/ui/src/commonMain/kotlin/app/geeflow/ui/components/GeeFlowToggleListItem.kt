package app.geeflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowTheme

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

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    GeeFlowToggleListItem(
        title = "Toggle List Item",
        subtitle = "Subtitle describing the toggle option.",
        checked = true,
        onCheckedChanged = {},
    )
}
