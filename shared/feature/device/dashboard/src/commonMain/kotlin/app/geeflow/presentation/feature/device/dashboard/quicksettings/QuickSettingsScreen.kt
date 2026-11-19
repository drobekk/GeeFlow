package app.geeflow.presentation.feature.device.dashboard.quicksettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import app.geeflow.ui.text.ClearText
import app.geeflow.ui.text.clearText
import app.geeflow.ui.theme.ClearScreenTheme
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_brew_boiler
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_current_temperature
import geeflow.shared.core.ui.generated.resources.common_steam_boiler
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

    ClearScreenTheme {
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
}

@Composable
private fun QuickSettingsContent(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit,
) = Column(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.large)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    GeeFlowDialogTopBar(
        title = clearText(ClearText.BoilerTemps),
        onCloseClick = { onEvent(QuickSettingsEvent.CloseClicked) },
    )
    VerticalSpacer(12.dp)
    QuickBoilerCard(
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

        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(12.dp)
    QuickBoilerCard(
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

        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun QuickBoilerCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    value: String,
    items: List<String>,
    unit: String,
    onCheckedChanged: (Boolean) -> Unit,
    onValueConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        val narrow = maxWidth < 400.dp
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (narrow) Text(subtitle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            GeeFlowToggleListItem(
                title = if (narrow) clearText(ClearText.Target) else clearText(ClearText.TargetTemp),
                subtitle = if (narrow) "" else subtitle,
                checked = checked,
                value = value,
                items = items,
                unit = unit,
                initialValue = value.removeSuffix(unit).trim().toFloatOrNull(),
                adjustmentStep = 1f,
                onCheckedChanged = onCheckedChanged,
                applyOnSelection = true,
                onValueConfirmed = onValueConfirmed,
                contentPadding = PaddingValues(top = 12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
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
