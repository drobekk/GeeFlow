package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.GeeFlowThemePreview
import dev.drobek.geeflow.ui.theme.disabled

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
) {
    GeeFlowListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        contentPadding = contentPadding,
        trailingContent = {
            val tint = if (enabled) LocalContentColor.current else LocalContentColor.current.disabled()
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = tint,
            )
        },
    )
}

@Composable
@Preview
private fun PreviewLight() = GeeFlowThemePreview(false) {
    GeeFlowNavigationListItem(
        title = "Navigation List Item",
        subtitle = "Subtitle describing the navigation option.",
    )
}

@Composable
@Preview
private fun PreviewDark() = GeeFlowThemePreview(true) {
    GeeFlowNavigationListItem(
        title = "Navigation List Item",
        subtitle = "Subtitle describing the navigation option.",
    )
}
