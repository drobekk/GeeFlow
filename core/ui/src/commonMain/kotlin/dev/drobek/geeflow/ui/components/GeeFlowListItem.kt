package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.GeeFlowThemePreview

@Composable
fun GeeFlowListItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = 16.dp,
    ),
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(modifier = modifier.padding(contentPadding), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            VerticalSpacer(4.dp)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailingContent != null) {
            HorizontalSpacer(16.dp)
            trailingContent()
        }
    }
}

@Composable
@Preview
private fun PreviewLight() = GeeFlowThemePreview(false) {
    GeeFlowListItem(
        title = "List Item Title",
        subtitle = "List Item Subtitle",
        trailingContent = {
            Text("Trailing")
        },
    )
}

@Composable
@Preview
private fun PreviewDark() = GeeFlowThemePreview(true) {
    GeeFlowListItem(
        title = "List Item Title",
        subtitle = "List Item Subtitle",
    )
}
