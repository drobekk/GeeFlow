package dev.drobek.geeflow.presentation.feature.device.quicksettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.DeviceNavigation
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.HorizontalSpacer
import dev.drobek.geeflow.ui.VerticalSpacer
import dev.drobek.geeflow.ui.components.GeeFlowInfinitePicker
import dev.drobek.geeflow.ui.components.GeeFlowSwitch
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_brew_boiler
import geeflow.composeapp.generated.resources.common_confirm
import geeflow.composeapp.generated.resources.common_steam_boiler
import geeflow.composeapp.generated.resources.quick_settings_more
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun QuickSettingsScreen(
    viewModel: QuickSettingsViewModel,
    navigator: DeviceNavigation
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.Back -> navigator.back()
        }
    }

    QuickSettingsContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent
    )
}

@Composable
private fun QuickSettingsContent(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit
) = Column(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
        .padding(24.dp)
        .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        BrewBoilerControls(viewState, onEvent)
        SteamBoilerControls(viewState, onEvent)
    }
    VerticalSpacer(24.dp)
    Buttons(onEvent)
}

@Composable
private fun RowScope.BrewBoilerControls(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit
) = Column(
    modifier = Modifier.weight(1f),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    BoilerHeader(
        label = stringResource(Res.string.common_brew_boiler),
        actualTemp = viewState.actualBrewTemp,
    )
    VerticalSpacer(16.dp)
    GeeFlowSwitch(
        checked = viewState.brewBoilerEnabled,
        onCheckedChange = { onEvent(QuickSettingsEvent.BrewBoilerToggled(it)) },
        enabled = true
    )
    VerticalSpacer(8.dp)
    GeeFlowInfinitePicker(
        items = viewState.brewTempList,
        selected = viewState.selectedBrewTemp,
        enabled = viewState.brewBoilerEnabled,
        onSelectionChanged = { onEvent(QuickSettingsEvent.BrewTempChanged(it)) }
    )
}

@Composable
private fun RowScope.SteamBoilerControls(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit
) = Column(
    modifier = Modifier.weight(1f),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    BoilerHeader(
        label = stringResource(Res.string.common_steam_boiler),
        actualTemp = viewState.actualSteamTemp,
    )
    VerticalSpacer(16.dp)
    GeeFlowSwitch(
        checked = viewState.steamBoilerEnabled,
        onCheckedChange = { onEvent(QuickSettingsEvent.SteamBoilerToggled(it)) },
        enabled = true
    )
    VerticalSpacer(8.dp)
    GeeFlowInfinitePicker(
        items = viewState.steamTempList,
        selected = viewState.selectedSteamTemp,
        enabled = viewState.steamBoilerEnabled,
        onSelectionChanged = { onEvent(QuickSettingsEvent.SteamTempChanged(it)) }
    )
}

@Composable
private fun BoilerHeader(
    label: String,
    actualTemp: Float,
) = FlowRow(
    verticalArrangement = Arrangement.Center,
    horizontalArrangement = Arrangement.Center
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    HorizontalSpacer(4.dp)
    Text(
        text = "(${actualTemp}°)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Buttons(
    onEvent: (QuickSettingsEvent) -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    TextButton(
        onClick = { onEvent(QuickSettingsEvent.MoreSettingsClicked) },
        content = { Text(stringResource(Res.string.quick_settings_more)) }
    )
    Button(
        onClick = { onEvent(QuickSettingsEvent.SaveClicked) },
        content = { Text(stringResource(Res.string.common_confirm)) }
    )
}

@Composable
@Preview
private fun QuickSettingsPreviewLight() = GeeFlowTheme(false) {
    QuickSettingsContent(
        viewState = QuickSettingsViewState(
            steamBoilerEnabled = true,
            brewBoilerEnabled = false,
            actualBrewTemp = 93.5f,
            actualSteamTemp = 125.0f,
            selectedBrewTemp = "93",
            selectedSteamTemp = "125"
        ),
        onEvent = {}
    )
}

@Composable
@Preview
private fun QuickSettingsPreviewDark() = GeeFlowTheme(true) {
    QuickSettingsContent(
        viewState = QuickSettingsViewState(
            steamBoilerEnabled = false,
            brewBoilerEnabled = true,
            actualBrewTemp = 93.5f,
            actualSteamTemp = 25.0f,
            selectedBrewTemp = "93",
            selectedSteamTemp = "125"
        ),
        onEvent = {}
    )
}
