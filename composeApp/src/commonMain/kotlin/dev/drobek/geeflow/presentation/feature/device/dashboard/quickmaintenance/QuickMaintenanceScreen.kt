package dev.drobek.geeflow.presentation.feature.device.dashboard.quickmaintenance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDashboardNavigation
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.VerticalSpacer
import dev.drobek.geeflow.ui.WaveOrientation
import dev.drobek.geeflow.ui.components.GeeFlowDialogTopBar
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.waveBackground
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_close
import geeflow.composeapp.generated.resources.common_cycle
import geeflow.composeapp.generated.resources.common_flush
import geeflow.composeapp.generated.resources.common_rest
import geeflow.composeapp.generated.resources.common_sec
import geeflow.composeapp.generated.resources.common_times
import geeflow.composeapp.generated.resources.device_water_alarm_error
import geeflow.composeapp.generated.resources.quick_maintenance_start
import geeflow.composeapp.generated.resources.quick_maintenance_stop
import geeflow.composeapp.generated.resources.quick_maintenance_title
import geeflow.composeapp.generated.resources.settings_quick_more
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun QuickMaintenanceScreen(
    viewModel: QuickMaintenanceViewModel,
    navigator: DeviceDashboardNavigation
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.Back -> navigator.back()
            is Navigation.MaintenanceSettings -> {
                navigator.back()
                navigator.showDeviceSettings(it.deviceId)
                navigator.showMaintenanceSettings(it.deviceId)
            }
        }
    }

    QuickMaintenanceContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent
    )
}

@Composable
private fun QuickMaintenanceContent(
    viewState: QuickMaintenanceViewState,
    onEvent: (QuickMaintenanceEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GeeFlowDialogTopBar(
            title = stringResource(Res.string.quick_maintenance_title),
            onCloseClick = { onEvent(QuickMaintenanceEvent.CloseClicked) }
        )
        VerticalSpacer(16.dp)
        if (viewState.waterLevelAlarm) {
            WaterAlarmContent()
        } else {
            CleaningProgressContent(viewState)
        }
        VerticalSpacer(32.dp)
        Buttons(viewState, onEvent)
    }
}

@Composable
private fun WaterAlarmContent() {
    Text(
        text = stringResource(Res.string.device_water_alarm_error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(16.dp)
    )
}

@Composable
private fun CleaningProgressContent(viewState: QuickMaintenanceViewState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressItem(
            label = stringResource(Res.string.common_flush),
            current = viewState.flushProgress.current,
            target = viewState.flushProgress.target,
            unit = stringResource(Res.string.common_sec),
            modifier = Modifier.weight(1f)
        )
        ProgressItem(
            label = stringResource(Res.string.common_rest),
            current = viewState.restProgress.current,
            target = viewState.restProgress.target,
            unit = stringResource(Res.string.common_sec),
            modifier = Modifier.weight(1f)
        )
        ProgressItem(
            label = stringResource(Res.string.common_cycle),
            current = viewState.cycleProgress.current,
            target = viewState.cycleProgress.target,
            unit = stringResource(Res.string.common_times),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProgressItem(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    modifier: Modifier = Modifier
) {
    val targetProgress = if (target > 0) current.toFloat() / target else 0f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .waveBackground(
                targetProgress = targetProgress,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                amplitude = 2.dp,
                orientation = WaveOrientation.Vertical,
                progressAnimationDurationMillis = 1000
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$current | $target",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Buttons(
    viewState: QuickMaintenanceViewState,
    onEvent: (QuickMaintenanceEvent) -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
) {
    TextButton(
        onClick = { onEvent(QuickMaintenanceEvent.MoreSettingsClicked) },
        content = { Text(stringResource(Res.string.settings_quick_more)) }
    )
    if (viewState.waterLevelAlarm) {
        Button(onClick = { onEvent(QuickMaintenanceEvent.CloseClicked) }) {
            Text(stringResource(Res.string.common_close))
        }
    } else {
        val buttonColor by animateColorAsState(
            if (viewState.isCleaning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary
        )
        val textColor by animateColorAsState(
            if (viewState.isCleaning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
        )
        Button(
            onClick = { onEvent(QuickMaintenanceEvent.ToggleCleaningClicked) },
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = textColor
            )
        ) {
            Text(stringResource(if (viewState.isCleaning) Res.string.quick_maintenance_stop else Res.string.quick_maintenance_start))
        }
    }
}

@Composable
@Preview
private fun PreviewCleaning() = GeeFlowTheme(false) {
    QuickMaintenanceContent(
        viewState = QuickMaintenanceViewState(
            isCleaning = true,
            flushProgress = QuickMaintenanceViewState.Progress(3, 5),
            restProgress = QuickMaintenanceViewState.Progress(0, 5),
            cycleProgress = QuickMaintenanceViewState.Progress(1, 3)
        ),
        onEvent = {}
    )
}

@Composable
@Preview
private fun PreviewWaterAlarm() = GeeFlowTheme(true) {
    QuickMaintenanceContent(
        viewState = QuickMaintenanceViewState(waterLevelAlarm = true),
        onEvent = {}
    )
}
