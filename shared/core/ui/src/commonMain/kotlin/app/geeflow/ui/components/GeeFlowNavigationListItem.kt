package app.geeflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.disabled

@Composable
fun GeeFlowNavigationListItem(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = 16.dp,
    ),
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    GeeFlowListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        contentPadding = contentPadding,
        trailingContent = trailingContent ?: {
            val tint = if (enabled) LocalContentColor.current else LocalContentColor.current.disabled()
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = tint,
            )
        },
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    GeeFlowNavigationListItem(
        title = "Navigation List Item",
        subtitle = "Subtitle describing the navigation option.",
    )
}
