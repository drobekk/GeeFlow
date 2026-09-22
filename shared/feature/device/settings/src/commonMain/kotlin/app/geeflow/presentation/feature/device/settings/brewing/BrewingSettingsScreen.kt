package app.geeflow.presentation.feature.device.settings.brewing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewBoilerToggled
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewTempChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PaddlePressureChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PaddleTimeChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PulseHeatingToggled
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamBoilerToggled
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamTempChanged
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewState.Boiler
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewState.Paddle
import app.geeflow.presentation.feature.device.settings.components.SettingsApplyFab
import app.geeflow.presentation.feature.device.settings.components.SettingsApplyFabPadding
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowDetailScaffold
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.icons.Flush
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.isWidthLarge
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_brew_boiler
import geeflow.shared.core.ui.generated.resources.common_current_temperature
import geeflow.shared.core.ui.generated.resources.common_pressure
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.common_steam_boiler
import geeflow.shared.core.ui.generated.resources.common_time
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_boiler
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_paddle
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_paddle_pressure_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_paddle_time_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_pulse_heating
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_pulse_heating_description
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun BrewingSettingsScreen(
    viewModel: BrewingSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BrewSettingsContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent,
    )

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is ShowSnackbar -> coroutineScope.launch { snackbarHostState.showSnackbar(it.message) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrewSettingsContent(
    viewState: BrewingSettingsViewState,
    onEvent: (BrewingSettingsEvent) -> Unit = {},
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowDetailScaffold(
        title = stringResource(Res.string.device_settings_brewing),
        subtitle = stringResource(Res.string.device_settings_brewing_description),
        navIconClick = { onEvent(CloseClicked) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            SettingsApplyFab(
                loading = viewState.applyButtonLoading,
                visible = viewState.applyButtonVisible,
                onClick = { onEvent(BrewingSettingsEvent.ApplyClicked) },
            )
        },
        scrollBehavior = scrollBehavior,
        content = {
            AdaptiveContent(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(it)
                    .padding(bottom = SettingsApplyFabPadding),
            )
        },
        modifier = Modifier
            .imePadding(),
    )
}

@Composable
private fun AdaptiveContent(
    viewState: BrewingSettingsViewState,
    onEvent: (BrewingSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        if (isWidthLarge()) {
            Row(
                Modifier.padding(
                    horizontal = GeeFlowTheme.spacing.contentHorizontal,
                    vertical = GeeFlowTheme.spacing.contentVertical,
                ),
            ) {
                Column(Modifier.weight(1f)) { BoilerSection(viewState, onEvent) }
                HorizontalSpacer(24.dp)
                Column(Modifier.weight(1f)) { PaddleSection(viewState.paddle, onEvent) }
            }
        } else {
            CompactContent(viewState, onEvent)
        }
    }
}

@Composable
private fun CompactContent(
    viewState: BrewingSettingsViewState,
    onEvent: (BrewingSettingsEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = GeeFlowTheme.spacing.contentHorizontal,
                vertical = GeeFlowTheme.spacing.contentVertical,
            ),
    ) {
        BoilerSection(viewState, onEvent)
        VerticalSpacer(24.dp)
        HorizontalDivider()
        VerticalSpacer(24.dp)
        PaddleSection(viewState.paddle, onEvent)
    }
}

@Composable
private fun BoilerSection(
    viewState: BrewingSettingsViewState,
    onEvent: (BrewingSettingsEvent) -> Unit,
) {
    SectionTitle(
        text = stringResource(Res.string.device_settings_brewing_boiler),
        modifier = Modifier.fillMaxWidth(),
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
        onCheckedChanged = { onEvent(BrewBoilerToggled(it)) },
        onValueConfirmed = { onEvent(BrewTempChanged(it)) },
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
        onCheckedChanged = { onEvent(SteamBoilerToggled(it)) },
        onValueConfirmed = { onEvent(SteamTempChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowToggleListItem(
        title = stringResource(Res.string.device_settings_brewing_pulse_heating),
        subtitle = stringResource(Res.string.device_settings_brewing_pulse_heating_description),
        checked = viewState.pulseHeatingEnabled,
        onCheckedChanged = { onEvent(PulseHeatingToggled(it)) },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
    )
}

@Composable
private fun PaddleSection(
    paddle: Paddle,
    onEvent: (BrewingSettingsEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            text = stringResource(Res.string.device_settings_brewing_paddle),
            modifier = Modifier.weight(1f, fill = false),
        )
        HorizontalSpacer(8.dp)
        Icon(
            imageVector = GeeFlowIcon.Flush,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .padding(6.dp)
                .size(20.dp),
        )
    }
    VerticalSpacer(16.dp)
    GeeFlowToggleListItem(
        title = stringResource(CoreRes.string.common_pressure),
        subtitle = stringResource(Res.string.device_settings_brewing_paddle_pressure_description),
        checked = true,
        value = "${paddle.pressure} ${stringResource(CoreRes.string.unit_bar)}",
        items = paddle.pressureList,
        unit = stringResource(CoreRes.string.unit_bar),
        onCheckedChanged = {},
        onValueConfirmed = { onEvent(PaddlePressureChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowToggleListItem(
        title = stringResource(CoreRes.string.common_time),
        subtitle = stringResource(Res.string.device_settings_brewing_paddle_time_description),
        checked = paddle.time != "0",
        value = "${paddle.time} ${stringResource(CoreRes.string.common_sec)}",
        items = paddle.timeList,
        unit = stringResource(CoreRes.string.common_sec),
        onCheckedChanged = { onEvent(PaddleTimeChanged(if (it) "30" else "0")) },
        onValueConfirmed = { onEvent(PaddleTimeChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) = Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier,
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    BrewSettingsContent(
        viewState = BrewingSettingsViewState(
            applyButtonVisible = true,
            brewBoiler = Boiler(
                enabled = true,
                actualTemp = 93.0f,
                selectedTemp = "93.0",
                tempList = (85..100).map { "$it.0" },
            ),
            steamBoiler = Boiler(
                enabled = true,
                actualTemp = 130.0f,
                selectedTemp = "130.0",
                tempList = (120..140).map { "$it.0" },
            ),
            paddle = Paddle(
                pressure = "9.0",
                pressureList = (0..12).map { "$it.0" },
                time = "30",
                timeList = (1..60).map { "$it" },
            ),
            pulseHeatingEnabled = true,
        ),
    )
}
