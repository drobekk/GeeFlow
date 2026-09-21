package app.geeflow.presentation.feature.device.dashboard.quicksettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewState.Boiler
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_brew_boiler
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_current_temperature
import geeflow.shared.core.ui.generated.resources.common_steam_boiler
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.settings_quick_more
import geeflow.shared.feature.device.dashboard.generated.resources.settings_quick_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun QuickSettingsScreen(
    viewModel: QuickSettingsViewModel,
    navigator: Navigator,
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

    Box {
        QuickSettingsContent(
            viewState = viewState,
            onEvent = viewModel::handleEvent,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun QuickSettingsContent(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit,
    isExpanded: Boolean = isWidthExpanded(),
) = Column(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.large)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    GeeFlowDialogTopBar(
        title = stringResource(Res.string.settings_quick_title),
        onCloseClick = { onEvent(QuickSettingsEvent.CloseClicked) },
    )
    VerticalSpacer(16.dp)
    GeeFlowToggleListItem(
        title = stringResource(CoreRes.string.common_brew_boiler),
        subtitle = stringResource(
            CoreRes.string.common_current_temperature,
            "${viewState.brewBoiler.actualTemp}°",
        ),
        checked = viewState.brewBoiler.enabled,
        value = "${viewState.brewBoiler.selectedTemp}°",
        items = viewState.brewBoiler.tempList,
        unit = "°",
        onCheckedChanged = { onEvent(QuickSettingsEvent.BrewBoilerToggled(it)) },
        onValueConfirmed = { onEvent(QuickSettingsEvent.BrewTempChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowToggleListItem(
        title = stringResource(CoreRes.string.common_steam_boiler),
        subtitle = stringResource(
            CoreRes.string.common_current_temperature,
            "${viewState.steamBoiler.actualTemp}°",
        ),
        checked = viewState.steamBoiler.enabled,
        value = "${viewState.steamBoiler.selectedTemp}°",
        items = viewState.steamBoiler.tempList,
        unit = "°",
        onCheckedChanged = { onEvent(QuickSettingsEvent.SteamBoilerToggled(it)) },
        onValueConfirmed = { onEvent(QuickSettingsEvent.SteamTempChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(24.dp)
    Buttons(viewState.applying, isExpanded, onEvent)
}

@Composable
private fun Buttons(
    applying: Boolean,
    isExpanded: Boolean,
    onEvent: (QuickSettingsEvent) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    TextButton(
        onClick = { onEvent(QuickSettingsEvent.MoreSettingsClicked(isExpanded)) },
        content = { Text(stringResource(Res.string.settings_quick_more)) },
    )
    Button(
        onClick = { if (!applying) onEvent(QuickSettingsEvent.ConfirmClicked) },
        content = {
            AnimatedContent(applying) {
                if (it) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(CoreRes.string.common_confirm))
                }
            }
        },
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    QuickSettingsContent(
        viewState = QuickSettingsViewState(
            steamBoiler = Boiler(
                enabled = true,
                actualTemp = 122.0f,
                selectedTemp = "125",
            ),
            brewBoiler = Boiler(
                enabled = false,
                actualTemp = 93.5f,
                selectedTemp = "93",
            ),
        ),
        onEvent = {},
    )
}
