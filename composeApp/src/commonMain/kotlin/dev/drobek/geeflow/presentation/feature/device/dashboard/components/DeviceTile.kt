package dev.drobek.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.DeviceDashboardViewState.Device
import dev.drobek.geeflow.ui.icons.GeeFlowIcon
import dev.drobek.geeflow.ui.icons.Pressure
import dev.drobek.geeflow.ui.icons.Steam
import dev.drobek.geeflow.ui.icons.Temperature

@Composable
internal fun DeviceTile(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = { onEvent(DeviceClicked) })
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(start = 3.dp)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                painter = rememberVectorPainter(Icons.Filled.ArrowDropDown),
                tint = MaterialTheme.colorScheme.outline,
                contentDescription = null
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ParameterItem(
                value = device.brewBoilerTemp,
                painter = rememberVectorPainter(GeeFlowIcon.Temperature),
            )
            ParameterItem(
                value = device.steamBoilerTemp,
                painter = rememberVectorPainter(GeeFlowIcon.Steam)
            )
            ParameterItem(
                value = device.pressure,
                painter = rememberVectorPainter(GeeFlowIcon.Pressure)
            )
        }
    }
}

@Composable
private fun ParameterItem(
    value: String?,
    painter: Painter,
    modifier: Modifier = Modifier
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.outline
    )
    Text(
        text = value ?: "—",
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.labelLarge
    )
}
