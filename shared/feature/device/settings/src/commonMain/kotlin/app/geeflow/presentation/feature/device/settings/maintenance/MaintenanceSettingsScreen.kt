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
import app.geeflow.data.device.model.CleaningReminder
import app.geeflow.data.device.model.CleaningType
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
import app.geeflow.ui.isWidthLarge
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_cycle
import geeflow.shared.core.ui.generated.resources.common_flush
import geeflow.shared.core.ui.generated.resources.common_rest
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.common_times
import geeflow.shared.core.ui.generated.resources.maintenance_daily
import geeflow.shared.core.ui.generated.resources.maintenance_daily_description
import geeflow.shared.core.ui.generated.resources.maintenance_days
import geeflow.shared.core.ui.generated.resources.maintenance_deep
import geeflow.shared.core.ui.generated.resources.maintenance_deep_description
import geeflow.shared.core.ui.generated.resources.maintenance_reminder
import geeflow.shared.core.ui.generated.resources.maintenance_reminder_days
import geeflow.shared.core.ui.generated.resources.maintenance_reminder_description
import geeflow.shared.core.ui.generated.resources.maintenance_reminder_interval
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_alarms
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_cleaning_cycle_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_cleaning_flush_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_cleaning_rest_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_water_alarm
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_water_alarm_description
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
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
        if (isWidthLarge()) {
            Row(
                Modifier.padding(
                    vertical = GeeFlowTheme.spacing.contentVertical,
                ),
            ) {
                Column(Modifier.weight(1f)) { CleaningSections(viewState, onEvent) }
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
            vertical = GeeFlowTheme.spacing.contentVertical,
        ),
    ) {
        CleaningSections(viewState, onEvent)
        VerticalSpacer(24.dp)
        HorizontalDivider(modifier = Modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal))
        VerticalSpacer(24.dp)
        WaterAlarmSection(viewState.waterAlarm, onEvent)
    }
}

@Composable
private fun CleaningSections(viewState: MaintenanceSettingsViewState, onEvent: (MaintenanceSettingsEvent) -> Unit) {
    CleaningSection(
        type = CleaningType.Daily,
        cleaning = viewState.cleaning,
        onEvent = onEvent,
    )
    VerticalSpacer(24.dp)
    HorizontalDivider(modifier = Modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal))
    VerticalSpacer(24.dp)
    CleaningSection(
        type = CleaningType.Deep,
        cleaning = viewState.deepCleaning,
        onEvent = onEvent,
    )
}

@Composable
private fun CleaningSection(
    type: CleaningType,
    cleaning: MaintenanceSettingsViewState.Cleaning,
    onEvent: (MaintenanceSettingsEvent) -> Unit,
) {
    SectionTitle(
        text = stringResource(
            if (type == CleaningType.Daily) CoreRes.string.maintenance_daily else CoreRes.string.maintenance_deep,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    VerticalSpacer(8.dp)
    Text(
        text = stringResource(
            if (type == CleaningType.Daily) {
                CoreRes.string.maintenance_daily_description
            } else {
                CoreRes.string.maintenance_deep_description
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = GeeFlowTheme.spacing.contentHorizontal),
    )
    VerticalSpacer(16.dp)
    GeeFlowValueListItem(
        title = stringResource(CoreRes.string.common_flush),
        subtitle = stringResource(Res.string.device_settings_maintenance_cleaning_flush_description),
        value = "${cleaning.timeSec} ${stringResource(CoreRes.string.common_sec)}",
        items = cleaning.timeList,
        unit = stringResource(CoreRes.string.common_sec),
        onValueConfirmed = { onEvent(CleaningTimeChanged(it, type)) },
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowValueListItem(
        title = stringResource(CoreRes.string.common_rest),
        subtitle = stringResource(Res.string.device_settings_maintenance_cleaning_rest_description),
        value = "${cleaning.restSec} ${stringResource(CoreRes.string.common_sec)}",
        items = cleaning.restList,
        unit = stringResource(CoreRes.string.common_sec),
        onValueConfirmed = { onEvent(CleaningRestChanged(it, type)) },
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowValueListItem(
        title = stringResource(CoreRes.string.common_cycle),
        subtitle = stringResource(Res.string.device_settings_maintenance_cleaning_cycle_description),
        value = "${cleaning.count} ${stringResource(CoreRes.string.common_times)}",
        items = cleaning.countList,
        unit = stringResource(CoreRes.string.common_times),
        onValueConfirmed = { onEvent(CleaningCountChanged(it, type)) },
        modifier = Modifier.fillMaxWidth(),
    )
    GeeFlowToggleListItem(
        title = stringResource(CoreRes.string.maintenance_reminder),
        subtitle = stringResource(CoreRes.string.maintenance_reminder_description),
        checked = cleaning.reminder.enabled,
        value = pluralStringResource(
            CoreRes.plurals.maintenance_reminder_interval,
            cleaning.reminder.intervalDays,
            cleaning.reminder.intervalDays,
        ),
        onCheckedChanged = {
            onEvent(MaintenanceSettingsEvent.ReminderChanged(type, cleaning.reminder.copy(enabled = it)))
        },
        onValueConfirmed = {
            onEvent(MaintenanceSettingsEvent.ReminderChanged(type, cleaning.reminder.copy(intervalDays = it.toInt())))
        },
        valueRange = 1f..CleaningReminder.MAX_INTERVAL_DAYS.toFloat(),
        unit = stringResource(CoreRes.string.maintenance_days),
        inputTitle = stringResource(CoreRes.string.maintenance_reminder_days),
        allowDecimal = false,
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
        contentPadding = PaddingValues(
            horizontal = GeeFlowTheme.spacing.contentHorizontal,
            vertical = 8.dp,
        ),
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
    modifier = modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal),
)

@Composable
private fun previewViewState() = MaintenanceSettingsViewState(
    cleaning = MaintenanceSettingsViewState.Cleaning(
        timeSec = "5",
        timeList = (1..30).map { it.toString() },
        restSec = "5",
        restList = (1..30).map { it.toString() },
        count = "3",
        countList = (1..10).map { it.toString() },
    ),
    deepCleaning = MaintenanceSettingsViewState.Cleaning(
        timeSec = "5",
        timeList = (1..30).map { it.toString() },
        restSec = "5",
        restList = (1..30).map { it.toString() },
        count = "6",
        countList = (1..10).map { it.toString() },
        reminder = CleaningReminder(enabled = true, intervalDays = 7),
    ),
    applyButtonVisible = true,
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    MaintenanceSettingsContent(previewViewState())
}
