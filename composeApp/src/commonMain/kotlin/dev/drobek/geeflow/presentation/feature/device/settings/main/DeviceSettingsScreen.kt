package dev.drobek.geeflow.presentation.feature.device.settings.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.presentation.feature.device.settings.BrewingSettings
import dev.drobek.geeflow.presentation.feature.device.settings.ConnectivitySettings
import dev.drobek.geeflow.presentation.feature.device.settings.MaintenanceSettings
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.ItemClicked
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewState.Item
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.components.GeeFlowTopBar
import dev.drobek.geeflow.ui.components.WaveDivider
import dev.drobek.geeflow.ui.components.scrollFade
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.modifier.WaveOrientation
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.compactSpacing
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_go_back
import geeflow.composeapp.generated.resources.device_settings_brewing
import geeflow.composeapp.generated.resources.device_settings_brewing_description
import geeflow.composeapp.generated.resources.device_settings_connectivity
import geeflow.composeapp.generated.resources.device_settings_connectivity_description
import geeflow.composeapp.generated.resources.device_settings_maintenance
import geeflow.composeapp.generated.resources.device_settings_maintenance_description
import geeflow.composeapp.generated.resources.device_settings_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSettingsScreen(
    viewModel: DeviceSettingsViewModel,
    navigator: Navigator
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val selectedIndex = when (navigator.getCurrentDestination()) {
        is BrewingSettings -> viewState.items.indexOfFirst { it is Item.Brewing }
        is MaintenanceSettings -> viewState.items.indexOfFirst { it is Item.Maintenance }
        is ConnectivitySettings -> viewState.items.indexOfFirst { it is Item.Connectivity }
        else -> null
    }.takeIf { it != null && it >= 0 }

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(
        viewState = viewState,
        selectedIndex = selectedIndex,
        onEvent = viewModel::handleEvent
    )
}

@Composable
private fun Content(
    viewState: DeviceSettingsViewState,
    selectedIndex: Int? = null,
    onEvent: (DeviceSettingsEvent) -> Unit = {},
) {
    if (isWidthExpanded()) {
        ExpandedContent(
            viewState = viewState,
            selectedIndex = selectedIndex,
            onEvent = onEvent
        )
    } else {
        CompactContent(
            viewState = viewState,
            onEvent = onEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedContent(
    viewState: DeviceSettingsViewState,
    selectedIndex: Int?,
    onEvent: (DeviceSettingsEvent) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            GeeFlowTopBar(
                title = viewState.deviceName,
                subtitle = stringResource(Res.string.device_settings_subtitle),
                navIconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                navIconContentDescription = stringResource(Res.string.common_go_back),
                navIconClick = { onEvent(BackClicked) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
                scrollBehavior = scrollBehavior
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .scrollFade(listState = listState, color = MaterialTheme.colorScheme.surfaceContainer),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
                itemsIndexed(viewState.items) { index, item ->
                    val selected = index == selectedIndex
                    SettingsItem(
                        item = item,
                        onEvent = onEvent,
                        backgroundColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .padding(
                                horizontal = compactSpacing().contentHorizontal,
                                vertical = 8.dp
                            )
                            .clip(MaterialTheme.shapes.large)
                    )
                }
            }
        }
        WaveDivider(
            color = MaterialTheme.colorScheme.surfaceContainer,
            orientation = WaveOrientation.Vertical
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactContent(
    viewState: DeviceSettingsViewState,
    onEvent: (DeviceSettingsEvent) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowScaffold(
        title = viewState.deviceName,
        subtitle = stringResource(Res.string.device_settings_subtitle),
        navIconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .fillMaxSize(),
                contentPadding = paddingValues + PaddingValues(
                    horizontal = GeeFlowTheme.spacing.contentHorizontal,
                    vertical = GeeFlowTheme.spacing.contentVertical
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewState.items) { item ->
                    SettingsItem(
                        item = item,
                        onEvent = onEvent,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    )
                }
                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
        }
    )
}

@Composable
private fun SettingsItem(
    item: Item,
    onEvent: (DeviceSettingsEvent) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .clickable(onClick = { onEvent(ItemClicked(item)) })
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
        Text(
            text = item.description,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
private fun previewViewState() = DeviceSettingsViewState(
    deviceName = "Data-S",
    items = listOf(
        Item.Brewing(
            stringResource(Res.string.device_settings_brewing),
            stringResource(Res.string.device_settings_brewing_description)
        ),
        Item.Maintenance(
            stringResource(Res.string.device_settings_maintenance),
            stringResource(Res.string.device_settings_maintenance_description)
        ),
        Item.Connectivity(
            stringResource(Res.string.device_settings_connectivity),
            stringResource(Res.string.device_settings_connectivity_description)
        )
    )
)

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    Content(previewViewState())
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    Content(previewViewState())
}
