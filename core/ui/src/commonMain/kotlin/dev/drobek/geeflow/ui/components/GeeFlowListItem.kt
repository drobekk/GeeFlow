package dev.drobek.geeflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.theme.GeeFlowTheme

@Composable
fun GeeFlowListItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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
private fun PreviewLight() = GeeFlowTheme(false) {
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
private fun PreviewDark() = GeeFlowTheme(true) {
    GeeFlowListItem(
        title = "List Item Title",
        subtitle = "List Item Subtitle",
    )
}
