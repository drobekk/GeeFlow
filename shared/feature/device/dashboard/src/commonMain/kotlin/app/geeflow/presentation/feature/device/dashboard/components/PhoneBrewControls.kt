package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText

@Composable
internal fun PhoneBrewControls(
    state: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    onDetails: () -> Unit,
    targetWeightGrams: Float? = null,
    stopAtYield: Boolean = false,
    onStartLiveRecipe: (() -> Unit)? = null,
) {
    val idle = state.brewButtonState == BrewButtonState.Idle
    val active = state.brewButtonState == BrewButtonState.Brewing
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val actions = buildList {
                add(clearText(ClearText.Flush) to DeviceDashboardEvent.ManualBrewClicked)
                add(clearText(ClearText.Manual) to DeviceDashboardEvent.FlowControlClicked)
                if (state.hotWaterSupported) add(clearText(ClearText.HotWater) to DeviceDashboardEvent.HotWaterClicked)
            }
            actions.forEach { (label, event) ->
                OutlinedButton(
                    onClick = { onEvent(event) },
                    enabled = idle,
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                ) { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
            }
            OutlinedButton(
                onClick = onDetails,
                modifier = Modifier.weight(1f).height(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
            ) { Text(clearText(ClearText.Details), style = MaterialTheme.typography.labelMedium) }
        }
        Button(
            onClick = {
                if (active) onEvent(DeviceDashboardEvent.StopBrewClicked)
                else if (onStartLiveRecipe != null) onStartLiveRecipe()
                else onEvent(DeviceDashboardEvent.BrewClicked(targetWeightGrams, stopAtYield))
            },
            enabled = idle || active,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
        ) {
            Text(
                when (state.brewButtonState) {
                    BrewButtonState.Idle -> clearText(ClearText.StartBrew)
                    BrewButtonState.Brewing -> if (state.device.brewStatus == DeviceDashboardViewState.Device.BrewStatus.Manual) {
                        clearText(ClearText.StopFlush)
                    } else {
                        clearText(ClearText.StopBrew)
                    }
                    BrewButtonState.Syncing -> clearText(ClearText.Syncing)
                    BrewButtonState.Busy -> clearText(ClearText.Busy)
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
