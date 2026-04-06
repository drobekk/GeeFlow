package dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewState.Boiler
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.components.GeeFlowDialogTopBar
import dev.drobek.geeflow.ui.components.GeeFlowInfinitePicker
import dev.drobek.geeflow.ui.components.GeeFlowSwitch
import dev.drobek.geeflow.ui.components.HorizontalSpacer
import dev.drobek.geeflow.ui.components.VerticalSpacer
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_brew_boiler
import geeflow.composeapp.generated.resources.common_confirm
import geeflow.composeapp.generated.resources.common_steam_boiler
import geeflow.composeapp.generated.resources.settings_quick_more
import geeflow.composeapp.generated.resources.settings_quick_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun QuickSettingsScreen(
    viewModel: QuickSettingsViewModel,
    navigator: Navigator
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is QuickSettingsViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(it.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        QuickSettingsContent(
            viewState = viewState,
            onEvent = viewModel::handleEvent
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun QuickSettingsContent(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit
) = Column(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.large)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    GeeFlowDialogTopBar(
        title = stringResource(Res.string.settings_quick_title),
        onCloseClick = { onEvent(QuickSettingsEvent.CloseClicked) }
    )
    VerticalSpacer(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Boiler(
            boiler = viewState.brewBoiler,
            label = stringResource(Res.string.common_brew_boiler),
            onTempChanged = { onEvent(QuickSettingsEvent.BrewTempChanged(it)) },
            onEnabledChanged = { onEvent(QuickSettingsEvent.BrewBoilerToggled(it)) },
            modifier = Modifier.weight(1f)
        )
        Boiler(
            boiler = viewState.steamBoiler,
            label = stringResource(Res.string.common_steam_boiler),
            onTempChanged = { onEvent(QuickSettingsEvent.SteamTempChanged(it)) },
            onEnabledChanged = { onEvent(QuickSettingsEvent.SteamBoilerToggled(it)) },
            modifier = Modifier.weight(1f)
        )
    }
    VerticalSpacer(24.dp)
    Buttons(viewState.applying, onEvent)
}

@Composable
private fun Boiler(
    boiler: Boiler,
    label: String,
    onTempChanged: (String) -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    BoilerHeader(
        label = label,
        actualTemp = boiler.actualTemp,
    )
    VerticalSpacer(16.dp)
    GeeFlowSwitch(
        checked = boiler.enabled,
        onCheckedChange = onEnabledChanged,
        enabled = true,
        modifier = Modifier.fillMaxWidth()
    )
    VerticalSpacer(8.dp)
    GeeFlowInfinitePicker(
        items = boiler.tempList,
        selected = boiler.selectedTemp,
        enabled = boiler.enabled,
        onSelectionChanged = onTempChanged
    )
}

@Composable
private fun BoilerHeader(
    label: String,
    actualTemp: Float,
) {
    val textMeasurer = rememberTextMeasurer()
    val textWidth = LocalDensity.current.run {
        textMeasurer.measure(text = "(100.0°)", style = MaterialTheme.typography.bodyMedium).size.width.toDp()
    }
    FlowRow(
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
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = textWidth)
        )
    }
}

@Composable
private fun Buttons(
    applying: Boolean,
    onEvent: (QuickSettingsEvent) -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    TextButton(
        onClick = { onEvent(QuickSettingsEvent.MoreSettingsClicked) },
        content = { Text(stringResource(Res.string.settings_quick_more)) }
    )
    Button(
        onClick = { if (!applying) onEvent(QuickSettingsEvent.ConfirmClicked) },
        content = {
            AnimatedContent(applying) {
                if (it) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(Res.string.common_confirm))
                }
            }
        }
    )
}

@Composable
@Preview
private fun QuickSettingsPreviewLight() = GeeFlowTheme(false) {
    QuickSettingsContent(
        viewState = QuickSettingsViewState(
            steamBoiler = Boiler(
                enabled = true,
                actualTemp = 122.0f,
                selectedTemp = "125"
            ),
            brewBoiler = Boiler(
                enabled = false,
                actualTemp = 93.5f,
                selectedTemp = "93"
            )
        ),
        onEvent = {}
    )
}

@Composable
@Preview
private fun QuickSettingsPreviewDark() = GeeFlowTheme(true) {
    QuickSettingsContent(
        viewState = QuickSettingsViewState(
            steamBoiler = Boiler(
                enabled = false,
                actualTemp = 122.0f,
                selectedTemp = "125"
            ),
            brewBoiler = Boiler(
                enabled = true,
                actualTemp = 93.5f,
                selectedTemp = "93"
            ),
            applying = true
        ),
        onEvent = {}
    )
}
