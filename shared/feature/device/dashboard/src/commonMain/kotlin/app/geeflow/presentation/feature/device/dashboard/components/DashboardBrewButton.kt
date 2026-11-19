package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_hot_water
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_manual_control
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.feature.device.dashboard.generated.resources.Res as DashboardRes

@Composable
internal fun DashboardBrewButton(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    targetWeightGrams: Float? = null,
    stopAtYield: Boolean = false,
    onStartLiveRecipe: (() -> Unit)? = null,
    startEnabled: Boolean = true,
) {
    val flushLabel = if (viewState.flushDurationSeconds > 0) {
        clearText(ClearText.FlushTimed, viewState.flushDurationSeconds)
    } else {
        clearText(ClearText.FlushUntimed)
    }
    val actions = buildList {
        add(
            BrewButtonAction(
                contentDescription = flushLabel,
                onClick = { onEvent(DeviceDashboardEvent.ManualBrewClicked) },
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null)
            },
        )
        add(
            BrewButtonAction(
                contentDescription = stringResource(DashboardRes.string.device_dashboard_manual_control),
                onClick = { onEvent(DeviceDashboardEvent.FlowControlClicked) },
            ) {
                Icon(Icons.Filled.SettingsSuggest, contentDescription = null)
            },
        )
        if (viewState.hotWaterSupported) {
            add(
                BrewButtonAction(
                    contentDescription = stringResource(Res.string.common_hot_water),
                    onClick = { onEvent(DeviceDashboardEvent.HotWaterClicked) },
                ) {
                    Icon(Icons.Outlined.WaterDrop, contentDescription = null)
                },
            )
        }
    }
    BrewButton(
        state = viewState.brewButtonState,
        startEnabled = startEnabled,
        stopContentDescription = if (viewState.device.brewStatus == DeviceDashboardViewState.Device.BrewStatus.Manual) {
            clearText(ClearText.StopFlush)
        } else {
            clearText(ClearText.StopBrew)
        },
        sideActions = actions,
        onHeroButtonClick = {
            if (onStartLiveRecipe != null) onStartLiveRecipe()
            else onEvent(DeviceDashboardEvent.BrewClicked(targetWeightGrams, stopAtYield))
        },
        onStopClick = { onEvent(DeviceDashboardEvent.StopBrewClicked) },
        modifier = modifier.fillMaxWidth(),
        heroIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
        stopIcon = { Icon(Icons.Filled.Stop, contentDescription = null) },
    )
}

@Composable
internal fun DashboardBrewControlsRail(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    targetWeightGrams: Float? = null,
    stopAtYield: Boolean = false,
    onStartLiveRecipe: (() -> Unit)? = null,
    onStopLiveRecipe: (() -> Unit)? = null,
    liveStopPending: Boolean = false,
    startEnabled: Boolean = true,
) {
    val state = viewState.brewButtonState
    val idle = state == BrewButtonState.Idle && !liveStopPending
    val active = state == BrewButtonState.Brewing || viewState.device.isBrewing || onStopLiveRecipe != null
    val flushLabel = if (viewState.flushDurationSeconds > 0) {
        clearText(ClearText.FlushTimed, viewState.flushDurationSeconds)
    } else {
        clearText(ClearText.FlushUntimed)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            shape = MaterialTheme.shapes.small,
            onClick = { onEvent(DeviceDashboardEvent.ManualBrewClicked) },
            enabled = idle,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        ) { Text(flushLabel, fontSize = 13.sp, maxLines = 1) }
        OutlinedButton(
            shape = MaterialTheme.shapes.small,
            onClick = { onEvent(DeviceDashboardEvent.FlowControlClicked) },
            enabled = idle,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        ) { Text(stringResource(DashboardRes.string.device_dashboard_manual_control), fontSize = 13.sp, maxLines = 1) }
        if (viewState.hotWaterSupported) {
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = { onEvent(DeviceDashboardEvent.HotWaterClicked) },
                enabled = idle,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) { Text(stringResource(Res.string.common_hot_water), fontSize = 13.sp, maxLines = 1) }
        }
        Button(
            shape = MaterialTheme.shapes.medium,
            onClick = {
                when {
                    onStopLiveRecipe != null -> onStopLiveRecipe()
                    active -> onEvent(DeviceDashboardEvent.StopBrewClicked)
                    onStartLiveRecipe != null -> onStartLiveRecipe()
                    else -> onEvent(DeviceDashboardEvent.BrewClicked(targetWeightGrams, stopAtYield))
                }
            },
            enabled = if (active) !liveStopPending else idle && startEnabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                contentColor = if (active) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.background,
            ),
        ) {
            Text(
                when {
                    liveStopPending -> clearText(ClearText.Busy)
                    active -> clearText(ClearText.StopBrew)
                    state == BrewButtonState.Syncing -> clearText(ClearText.Syncing)
                    state == BrewButtonState.Busy -> clearText(ClearText.Busy)
                    else -> clearText(ClearText.StartBrew)
                },
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
    }
}
