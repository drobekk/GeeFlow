package dev.drobek.geeflow.presentation.feature.device.settings.brewing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.BrewTempChanged
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PaddlePressureChanged
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.PulseHeatingToggled
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamBoilerToggled
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsEvent.SteamTempChanged
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsViewState.Boiler
import dev.drobek.geeflow.presentation.feature.device.settings.components.SettingsApplyFab
import dev.drobek.geeflow.presentation.feature.device.settings.components.SettingsApplyFabPadding
import dev.drobek.geeflow.presentation.feature.device.settings.components.SettingsToggleRow
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.components.GeeFlowInfinitePicker
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.components.GeeFlowSwitch
import dev.drobek.geeflow.ui.components.HorizontalSpacer
import dev.drobek.geeflow.ui.components.VerticalSpacer
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.isWidthLarge
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.core.ui.generated.resources.common_brew_boiler
import geeflow.core.ui.generated.resources.common_pressure
import geeflow.core.ui.generated.resources.common_steam_boiler
import geeflow.core.ui.generated.resources.common_time
import geeflow.feature.device.settings.generated.resources.Res
import geeflow.feature.device.settings.generated.resources.device_settings_brewing
import geeflow.feature.device.settings.generated.resources.device_settings_brewing_boiler
import geeflow.feature.device.settings.generated.resources.device_settings_brewing_description
import geeflow.feature.device.settings.generated.resources.device_settings_brewing_paddle
import geeflow.feature.device.settings.generated.resources.device_settings_brewing_pulse_heating
import geeflow.feature.device.settings.generated.resources.device_settings_brewing_pulse_heating_description
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun BrewingSettingsScreen(
    viewModel: BrewingSettingsViewModel,
    navigator: dev.drobek.geeflow.navigation.Navigator
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BrewSettingsContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent
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
    snackbarHostState: SnackbarHostState = SnackbarHostState()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    if (isWidthExpanded()) {
        ExpandedContent(
            viewState = viewState,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent
        )
    } else {
        GeeFlowScaffold(
            title = stringResource(Res.string.device_settings_brewing),
            subtitle = stringResource(Res.string.device_settings_brewing_description),
            navIconClick = { onEvent(CloseClicked) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                SettingsApplyFab(
                    loading = viewState.applyButtonLoading,
                    visible = viewState.applyButtonVisible,
                    onClick = { onEvent(BrewingSettingsEvent.ApplyClicked) }
                )
            },
            scrollBehavior = scrollBehavior,
            content = {
                CompactContent(
                    viewState = viewState,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(it)
                        .padding(bottom = SettingsApplyFabPadding)
                )
            },
            modifier = Modifier
                .imePadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        )
    }
}

@Composable
private fun ExpandedContent(
    viewState: BrewingSettingsViewState,
    snackbarHostState: SnackbarHostState,
    onEvent: (BrewingSettingsEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isWidthLarge()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = GeeFlowTheme.spacing.contentHorizontal,
                        vertical = GeeFlowTheme.spacing.contentVertical
                    )
                    .systemBarsPadding()
                    .padding(bottom = SettingsApplyFabPadding)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BoilerSection(
                        viewState = viewState,
                        onEvent = onEvent
                    )
                }
                HorizontalSpacer(24.dp)
                Column(modifier = Modifier.weight(1f)) {
                    PaddleSection(
                        paddle = viewState.paddle,
                        onEvent = onEvent,
                    )
                }
            }
        } else {
            CompactContent(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .systemBarsPadding()
                    .padding(bottom = SettingsApplyFabPadding)
            )
        }
        SettingsApplyFab(
            loading = viewState.applyButtonLoading,
            visible = viewState.applyButtonVisible,
            onClick = { onEvent(BrewingSettingsEvent.ApplyClicked) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.systemBarsPadding().align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CompactContent(
    viewState: BrewingSettingsViewState,
    onEvent: (BrewingSettingsEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = GeeFlowTheme.spacing.contentHorizontal,
                vertical = GeeFlowTheme.spacing.contentVertical
            )
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
    onEvent: (BrewingSettingsEvent) -> Unit
) {
    SectionTitle(
        text = stringResource(Res.string.device_settings_brewing_boiler),
        modifier = Modifier.fillMaxWidth()
    )
    VerticalSpacer(24.dp)
    Row {
        Boiler(
            boiler = viewState.brewBoiler,
            label = stringResource(CoreRes.string.common_brew_boiler),
            onTempChanged = { onEvent(BrewTempChanged(it)) },
            onEnabledChanged = { onEvent(BrewBoilerToggled(it)) },
            modifier = Modifier.weight(1f)
        )
        HorizontalSpacer(16.dp)
        Boiler(
            boiler = viewState.steamBoiler,
            label = stringResource(CoreRes.string.common_steam_boiler),
            onTempChanged = { onEvent(SteamTempChanged(it)) },
            onEnabledChanged = { onEvent(SteamBoilerToggled(it)) },
            modifier = Modifier.weight(1f)
        )
    }
    VerticalSpacer(24.dp)
    SettingsToggleRow(
        title = stringResource(Res.string.device_settings_brewing_pulse_heating),
        subtitle = stringResource(Res.string.device_settings_brewing_pulse_heating_description),
        checked = viewState.pulseHeatingEnabled,
        onCheckedChanged = { onEvent(PulseHeatingToggled(it)) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PaddleSection(
    paddle: BrewingSettingsViewState.Paddle,
    onEvent: (BrewingSettingsEvent) -> Unit
) {
    SectionTitle(
        text = stringResource(Res.string.device_settings_brewing_paddle),
        modifier = Modifier.fillMaxWidth()
    )
    VerticalSpacer(24.dp)
    Row {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(CoreRes.string.common_pressure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpacer(16.dp)
            GeeFlowInfinitePicker(
                items = paddle.pressureList,
                selected = paddle.pressure,
                enabled = true,
                onSelectionChanged = { onEvent(PaddlePressureChanged(it)) }
            )
        }
        HorizontalSpacer(16.dp)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(CoreRes.string.common_time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpacer(16.dp)
            GeeFlowInfinitePicker(
                items = paddle.timeList,
                selected = paddle.time,
                enabled = true,
                onSelectionChanged = { onEvent(BrewingSettingsEvent.PaddleTimeChanged(it)) }
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) = Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier
)

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
) = FlowRow(
    verticalArrangement = Arrangement.Center,
    horizontalArrangement = Arrangement.Start,
    modifier = Modifier.fillMaxWidth()
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    HorizontalSpacer(4.dp)
    Text(
        text = "($actualTemp°)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun previewViewState() = BrewingSettingsViewState(applyButtonVisible = true)

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    BrewSettingsContent(previewViewState())
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    BrewSettingsContent(previewViewState())
}
