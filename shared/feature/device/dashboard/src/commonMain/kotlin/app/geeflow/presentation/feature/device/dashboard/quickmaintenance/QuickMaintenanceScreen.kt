package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.WaveOrientation
import app.geeflow.ui.modifier.waveBackground
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_close
import geeflow.shared.core.ui.generated.resources.common_cycle
import geeflow.shared.core.ui.generated.resources.common_flush
import geeflow.shared.core.ui.generated.resources.common_rest
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.common_settings
import geeflow.shared.core.ui.generated.resources.common_times
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_water_alarm_error
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_start
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_stop
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_title
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun QuickMaintenanceScreen(
    viewModel: QuickMaintenanceViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NavigatorEffect(navigator, viewModel.navEvent)

    QuickMaintenanceContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun QuickMaintenanceContent(
    viewState: QuickMaintenanceViewState,
    onEvent: (QuickMaintenanceEvent) -> Unit,
    isExpanded: Boolean = isWidthExpanded(),
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.large)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GeeFlowDialogTopBar(
            title = stringResource(Res.string.quick_maintenance_title),
            onCloseClick = { onEvent(QuickMaintenanceEvent.CloseClicked) },
        )
        VerticalSpacer(16.dp)
        if (viewState.waterLevelAlarm) {
            WaterAlarmContent()
        } else {
            CleaningProgressContent(viewState)
        }
        VerticalSpacer(32.dp)
        Buttons(viewState, isExpanded, onEvent)
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
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
    )
}

@Composable
private fun CleaningProgressContent(viewState: QuickMaintenanceViewState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressItem(
            label = stringResource(CoreRes.string.common_flush),
            current = viewState.flushProgress.current,
            target = viewState.flushProgress.target,
            unit = stringResource(CoreRes.string.common_sec),
            modifier = Modifier.weight(1f),
        )
        ProgressItem(
            label = stringResource(CoreRes.string.common_rest),
            current = viewState.restProgress.current,
            target = viewState.restProgress.target,
            unit = stringResource(CoreRes.string.common_sec),
            modifier = Modifier.weight(1f),
        )
        ProgressItem(
            label = stringResource(CoreRes.string.common_cycle),
            current = viewState.cycleProgress.current,
            target = viewState.cycleProgress.target,
            unit = stringResource(CoreRes.string.common_times),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ProgressItem(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (target > 0) current.toFloat() / target else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "CleaningProgress",
    )

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .waveBackground(
                progress = animatedProgress,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                amplitude = 2.dp,
                orientation = WaveOrientation.Vertical,
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$current | $target",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Buttons(
    viewState: QuickMaintenanceViewState,
    isExpanded: Boolean,
    onEvent: (QuickMaintenanceEvent) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = spacedBy(16.dp, alignment = Alignment.End),
) {
    TextButton(
        onClick = { onEvent(QuickMaintenanceEvent.MoreSettingsClicked(isExpanded)) },
        content = { Text(stringResource(CoreRes.string.common_settings)) },
    )
    if (viewState.waterLevelAlarm) {
        Button(onClick = { onEvent(QuickMaintenanceEvent.CloseClicked) }) {
            Text(stringResource(CoreRes.string.common_close))
        }
    } else {
        val buttonColor by animateColorAsState(
            if (viewState.isCleaning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
        )
        val textColor by animateColorAsState(
            if (viewState.isCleaning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
        )
        Button(
            onClick = { onEvent(QuickMaintenanceEvent.ToggleCleaningClicked) },
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = textColor,
            ),
        ) {
            Text(
                stringResource(
                    if (viewState.isCleaning) Res.string.quick_maintenance_stop else Res.string.quick_maintenance_start,
                ),
            )
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    QuickMaintenanceContent(
        viewState = QuickMaintenanceViewState(
            isCleaning = true,
            flushProgress = QuickMaintenanceViewState.Progress(3, 5),
            restProgress = QuickMaintenanceViewState.Progress(0, 5),
            cycleProgress = QuickMaintenanceViewState.Progress(1, 3),
        ),
        onEvent = {},
    )
}
