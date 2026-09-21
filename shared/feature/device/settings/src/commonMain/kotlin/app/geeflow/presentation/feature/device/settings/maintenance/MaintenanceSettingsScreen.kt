package app.geeflow.presentation.feature.device.settings.maintenance

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.settings.components.SettingsApplyFab
import app.geeflow.presentation.feature.device.settings.components.SettingsApplyFabPadding
import app.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningCountChanged
import app.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningRestChanged
import app.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CleaningTimeChanged
import app.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsEvent.WaterAlarmToggled
import app.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsViewModelEvent.ShowSnackbar
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowDetailScaffold
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.components.GeeFlowValueListItem
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_cycle
import geeflow.shared.core.ui.generated.resources.common_flush
import geeflow.shared.core.ui.generated.resources.common_rest
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.common_times
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_alarms
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_cleaning
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_cleaning_cycle_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_cleaning_flush_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_cleaning_rest_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_water_alarm
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_water_alarm_description
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

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
    GeeFlowDetailScaffold(
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
    viewState: MaintenanceSettingsViewState,
    onEvent: (MaintenanceSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        if (maxWidth >= 640.dp) {
            Row(
                Modifier.padding(
                    horizontal = GeeFlowTheme.spacing.contentHorizontal,
                    vertical = GeeFlowTheme.spacing.contentVertical,
                ),
            ) {
                Column(Modifier.weight(1f)) { CleaningSection(viewState.cleaning, onEvent) }
                HorizontalSpacer(24.dp)
                Column(Modifier.weight(1f)) { WaterAlarmSection(viewState.waterAlarm, onEvent) }
            }
        } else {
            CompactContent(viewState, onEvent)
        }
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
    VerticalSpacer(16.dp)
    GeeFlowValueListItem(
        title = stringResource(CoreRes.string.common_flush),
        subtitle = stringResource(Res.string.device_settings_maintenance_cleaning_flush_description),
        value = "${cleaning.timeSec} ${stringResource(CoreRes.string.common_sec)}",
        items = cleaning.timeList,
        unit = stringResource(CoreRes.string.common_sec),
        onValueConfirmed = { onEvent(CleaningTimeChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowValueListItem(
        title = stringResource(CoreRes.string.common_rest),
        subtitle = stringResource(Res.string.device_settings_maintenance_cleaning_rest_description),
        value = "${cleaning.restSec} ${stringResource(CoreRes.string.common_sec)}",
        items = cleaning.restList,
        unit = stringResource(CoreRes.string.common_sec),
        onValueConfirmed = { onEvent(CleaningRestChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowValueListItem(
        title = stringResource(CoreRes.string.common_cycle),
        subtitle = stringResource(Res.string.device_settings_maintenance_cleaning_cycle_description),
        value = "${cleaning.count} ${stringResource(CoreRes.string.common_times)}",
        items = cleaning.countList,
        unit = stringResource(CoreRes.string.common_times),
        onValueConfirmed = { onEvent(CleaningCountChanged(it)) },
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    )
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
    VerticalSpacer(16.dp)
    GeeFlowToggleListItem(
        title = stringResource(resource = Res.string.device_settings_maintenance_water_alarm),
        subtitle = stringResource(Res.string.device_settings_maintenance_water_alarm_description),
        checked = waterAlarm,
        onCheckedChanged = { onEvent(WaterAlarmToggled(it)) },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
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

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    MaintenanceSettingsContent(previewViewState())
}
