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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.ItemClicked
import dev.drobek.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewState.Item
import dev.drobek.geeflow.presentation.feature.device.settings.navigation.DeviceSettingsNavigation
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.WaveOrientation
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.components.GeeFlowTopBar
import dev.drobek.geeflow.ui.components.WaveDivider
import dev.drobek.geeflow.ui.isWidthExpanded
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
import geeflow.composeapp.generated.resources.device_settings_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSettingsScreen(
    viewModel: DeviceSettingsViewModel,
    navigation: DeviceSettingsNavigation
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    Content(
        viewState = viewState,
        onEvent = viewModel::handleEvent
    )

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.Back -> navigation.back()
            is Navigation.BrewingSettings -> navigation.showBrewingSettings(it.deviceId)
            is Navigation.ConnectivitySettings -> navigation.showBrewingSettings(it.deviceId) // TODO
            is Navigation.MaintenanceSettings -> navigation.showBrewingSettings(it.deviceId) // TODO
        }
    }
}

@Composable
private fun Content(
    viewState: DeviceSettingsViewState,
    onEvent: (DeviceSettingsEvent) -> Unit = {},
) {
    val title = stringResource(Res.string.device_settings_title)
    val subtitle = stringResource(Res.string.device_settings_subtitle)
    val navIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack)
    val padding = compactSpacing()

    if (isWidthExpanded()) {
        Row(
            modifier = Modifier
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
                item {
                    GeeFlowTopBar(
                        title = title,
                        subtitle = subtitle,
                        navIconPainter = navIcon,
                        navIconContentDescription = stringResource(Res.string.common_go_back),
                        navIconClick = { onEvent(DeviceSettingsEvent.BackClicked) }
                    )
                }
                items(viewState.items) {
                    SettingsItem(
                        item = it,
                        onEvent = onEvent,
                        modifier = Modifier
                            .padding(
                                horizontal = padding.contentHorizontal,
                                vertical = 8.dp
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
            WaveDivider(
                color = MaterialTheme.colorScheme.surfaceContainer,
                orientation = WaveOrientation.Vertical
            )
        }
    } else {
        GeeFlowScaffold(
            title = title,
            subtitle = subtitle,
            navIconPainter = navIcon,
            navIconClick = { onEvent(DeviceSettingsEvent.BackClicked) },
            content = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = it + PaddingValues(
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        )
                    }
                    item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    item: Item,
    onEvent: (DeviceSettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onEvent(ItemClicked(item)) })
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = item.description,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun previewViewState() = DeviceSettingsViewState(
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
