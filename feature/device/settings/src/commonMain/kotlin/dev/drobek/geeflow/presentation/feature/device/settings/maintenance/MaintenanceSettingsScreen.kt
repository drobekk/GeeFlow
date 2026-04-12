package dev.drobek.geeflow.presentation.feature.device.settings.maintenance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.presentation.feature.device.settings.components.SettingsApplyFab
import dev.drobek.geeflow.presentation.feature.device.settings.components.SettingsApplyFabPadding
import dev.drobek.geeflow.presentation.feature.device.settings.components.SettingsToggleRow
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningCountChanged
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningRestChanged
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningTimeChanged
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.WaterAlarmToggled
import dev.drobek.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.components.GeeFlowInfinitePicker
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.components.HorizontalSpacer
import dev.drobek.geeflow.ui.components.VerticalSpacer
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.isWidthLarge
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.core.ui.generated.resources.common_cycle
import geeflow.core.ui.generated.resources.common_flush
import geeflow.core.ui.generated.resources.common_rest
import geeflow.core.ui.generated.resources.common_seconds
import geeflow.core.ui.generated.resources.common_times
import geeflow.feature.device.settings.generated.resources.Res
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance_alarms
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance_cleaning
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance_description
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance_water_alarm
import geeflow.feature.device.settings.generated.resources.device_settings_maintenance_water_alarm_description
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun MaintenanceSettingsScreen(
    viewModel: MaintenanceSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    MaintenanceSettingsContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent,
    )

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is ShowSnackbar -> coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(it.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceSettingsContent(
    viewState: MaintenanceSettingsViewState,
    onEvent: (MaintenanceSettingsEvent) -> Unit = {},
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    if (isWidthExpanded()) {
        ExpandedContent(
            viewState = viewState,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
        )
    } else {
        GeeFlowScaffold(
            title = stringResource(Res.string.device_settings_maintenance),
            subtitle = stringResource(Res.string.device_settings_maintenance_description),
            navIconClick = { onEvent(CloseClicked) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                SettingsApplyFab(
                    loading = viewState.applyButtonLoading,
                    visible = viewState.applyButtonVisible,
                    onClick = { onEvent(MaintenanceSettingsEvent.ApplyClicked) },
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
                        .padding(bottom = SettingsApplyFabPadding),
                )
            },
            modifier = Modifier
                .imePadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        )
    }
}

@Composable
private fun ExpandedContent(
    viewState: MaintenanceSettingsViewState,
    snackbarHostState: SnackbarHostState,
    onEvent: (MaintenanceSettingsEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isWidthLarge()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = GeeFlowTheme.spacing.contentHorizontal,
                        vertical = GeeFlowTheme.spacing.contentVertical,
                    )
                    .systemBarsPadding()
                    .padding(bottom = SettingsApplyFabPadding),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CleaningSection(viewState.cleaning, onEvent)
                }
                HorizontalSpacer(24.dp)
                Column(modifier = Modifier.weight(1f)) {
                    WaterAlarmSection(viewState.waterAlarm, onEvent)
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
                    .padding(bottom = SettingsApplyFabPadding),
            )
        }
        SettingsApplyFab(
            loading = viewState.applyButtonLoading,
            visible = viewState.applyButtonVisible,
            onClick = { onEvent(MaintenanceSettingsEvent.ApplyClicked) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding(),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.systemBarsPadding().align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CompactContent(
    viewState: MaintenanceSettingsViewState,
    onEvent: (MaintenanceSettingsEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            horizontal = GeeFlowTheme.spacing.contentHorizontal,
            vertical = GeeFlowTheme.spacing.contentVertical,
        ),
    ) {
        CleaningSection(viewState.cleaning, onEvent)
        VerticalSpacer(24.dp)
        HorizontalDivider()
        VerticalSpacer(24.dp)
        WaterAlarmSection(viewState.waterAlarm, onEvent)
    }
}

@Composable
private fun CleaningSection(
    cleaning: MaintenanceSettingsViewState.Cleaning,
    onEvent: (MaintenanceSettingsEvent) -> Unit,
) {
    SectionTitle(
        text = stringResource(Res.string.device_settings_maintenance_cleaning),
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(24.dp)
    Row {
        Picker(
            title = stringResource(CoreRes.string.common_flush),
            subtitle = stringResource(CoreRes.string.common_seconds),
            items = cleaning.timeList,
            selected = cleaning.timeSec,
            onSelectionChanged = { onEvent(CleaningTimeChanged(it)) },
            modifier = Modifier.weight(1f),
        )
        HorizontalSpacer(16.dp)
        Picker(
            title = stringResource(CoreRes.string.common_rest),
            subtitle = stringResource(CoreRes.string.common_seconds),
            items = cleaning.restList,
            selected = cleaning.restSec,
            onSelectionChanged = { onEvent(CleaningRestChanged(it)) },
            modifier = Modifier.weight(1f),
        )
        HorizontalSpacer(16.dp)
        Picker(
            title = stringResource(CoreRes.string.common_cycle),
            subtitle = stringResource(CoreRes.string.common_times),
            items = cleaning.countList,
            selected = cleaning.count,
            onSelectionChanged = { onEvent(CleaningCountChanged(it)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Picker(
    title: String,
    subtitle: String,
    items: List<String>,
    selected: String,

    onSelectionChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(12.dp)
        GeeFlowInfinitePicker(
            items = items,
            selected = selected,
            enabled = true,
            onSelectionChanged = onSelectionChanged,
        )
    }
}

@Composable
private fun WaterAlarmSection(
    waterAlarm: Boolean,
    onEvent: (MaintenanceSettingsEvent) -> Unit,
) {
    SectionTitle(
        text = stringResource(Res.string.device_settings_maintenance_alarms),
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(24.dp)
    SettingsToggleRow(
        title = stringResource(resource = Res.string.device_settings_maintenance_water_alarm),
        subtitle = stringResource(Res.string.device_settings_maintenance_water_alarm_description),
        checked = waterAlarm,
        onCheckedChanged = { onEvent(WaterAlarmToggled(it)) },
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

@Composable
private fun previewViewState() = MaintenanceSettingsViewState(applyButtonVisible = true)

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    MaintenanceSettingsContent(previewViewState())
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    MaintenanceSettingsContent(previewViewState())
}
