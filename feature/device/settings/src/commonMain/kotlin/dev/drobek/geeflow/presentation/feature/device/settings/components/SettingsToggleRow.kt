package dev.drobek.geeflow.presentation.feature.device.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.ui.components.GeeFlowSwitch
import dev.drobek.geeflow.ui.components.HorizontalSpacer
import dev.drobek.geeflow.ui.components.VerticalSpacer

@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
        HorizontalSpacer(16.dp)
        GeeFlowSwitch(
            checked = checked,
            onCheckedChange = onCheckedChanged,
            enabled = true,
        )
    }
}
