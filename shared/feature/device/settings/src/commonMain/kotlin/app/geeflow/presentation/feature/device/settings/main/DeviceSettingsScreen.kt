package app.geeflow.presentation.feature.device.settings.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.presentation.feature.device.settings.BrewingSettings
import app.geeflow.presentation.feature.device.settings.ConnectivitySettings
import app.geeflow.presentation.feature.device.settings.MaintenanceSettings
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.BackClicked
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.ItemClicked
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewState.Item
import app.geeflow.ui.components.GeeFlowListScaffold
import app.geeflow.ui.components.isListDetailExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance_description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSettingsScreen(
    viewModel: DeviceSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val destination = navigator.getCurrentDestination()
    val expanded = isListDetailExpanded()
    var initializedWidePane by rememberSaveable { mutableStateOf(false) }
    val selectedIndex = viewState.items.indexOfFirst { item ->
        when (item) {
            is Item.Brewing -> destination is BrewingSettings
            is Item.Maintenance -> destination is MaintenanceSettings
            is Item.Connectivity -> destination is ConnectivitySettings
            is Item.Preference -> destination == item.destination
        }
    }.takeIf { it >= 0 }

    NavigatorEffect(navigator, viewModel.navEvent)
    LaunchedEffect(expanded, viewState.items, destination) {
        if (expanded && !initializedWidePane && viewState.items.isNotEmpty()) {
            initializedWidePane = true
            if (destination is DeviceSettings && destination.entryPoint == null) {
                viewModel.handleEvent(ItemClicked(viewState.items.first()))
            }
        }
    }
    if (expanded) {
        NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None)) {
            viewModel.handleEvent(BackClicked)
        }
    }
    Content(
        viewState = viewState,
        selectedIndex = selectedIndex,
        onEvent = viewModel::handleEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewState: DeviceSettingsViewState,
    selectedIndex: Int? = null,
    onEvent: (DeviceSettingsEvent) -> Unit = {},
) {
    val expanded = isListDetailExpanded()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    GeeFlowListScaffold(
        plain = true,
        title = viewState.deviceName,
        navIconClick = { onEvent(BackClicked) },
        expanded = expanded,
        scrollBehavior = scrollBehavior,
        modifier = Modifier,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding + PaddingValues(
                horizontal = if (expanded) 24.dp else GeeFlowTheme.spacing.contentHorizontal,
                vertical = if (expanded) 8.dp else GeeFlowTheme.spacing.contentVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(viewState.items) { index, item ->
                val selected = expanded && index == selectedIndex
                SettingsItem(
                    item = item,
                    onEvent = onEvent,
                    backgroundColor = if (expanded) {
                        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    modifier = Modifier
                        .then(
                            if (selected) {
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    MaterialTheme.shapes.large
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clip(MaterialTheme.shapes.large),
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    item: Item,
    onEvent: (DeviceSettingsEvent) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .clickable(onClick = { onEvent(ItemClicked(item)) })
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        if (item.description.isNotBlank()) Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun previewViewState() = DeviceSettingsViewState(
    deviceName = "Data-S",
    items = listOf(
        Item.Brewing(
            stringResource(Res.string.device_settings_brewing),
            stringResource(Res.string.device_settings_brewing_description),
        ),
        Item.Maintenance(
            stringResource(Res.string.device_settings_maintenance),
            stringResource(Res.string.device_settings_maintenance_description),
        ),
        Item.Connectivity(
            stringResource(Res.string.device_settings_connectivity),
            stringResource(Res.string.device_settings_connectivity_description),
        ),
    ),
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    Content(previewViewState())
}
