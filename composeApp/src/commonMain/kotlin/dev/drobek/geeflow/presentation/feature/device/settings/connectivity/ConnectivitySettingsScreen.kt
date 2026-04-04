package dev.drobek.geeflow.presentation.feature.device.settings.connectivity

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.navigation.NavigatorEffect
import dev.drobek.geeflow.presentation.feature.device.settings.components.SettingsToggleRow
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.CloseClicked
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.RescanClicked
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.ScaleConnectionClicked
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.SmartScaleToggled
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.ScaleConnectionStatus
import dev.drobek.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.ScaleViewItem
import dev.drobek.geeflow.ui.components.HorizontalSpacer
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.isWidthExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_connect
import geeflow.composeapp.generated.resources.common_disconnect
import geeflow.composeapp.generated.resources.device_dashboard_connecting
import geeflow.composeapp.generated.resources.device_settings_connectivity
import geeflow.composeapp.generated.resources.device_settings_connectivity_description
import geeflow.composeapp.generated.resources.device_settings_connectivity_not_found
import geeflow.composeapp.generated.resources.device_settings_connectivity_rescan
import geeflow.composeapp.generated.resources.device_settings_connectivity_searching
import geeflow.composeapp.generated.resources.device_settings_connectivity_smart_scale
import geeflow.composeapp.generated.resources.device_settings_connectivity_smart_scale_description
import geeflow.composeapp.generated.resources.device_settings_connectivity_smart_scale_info
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConnectivitySettingsScreen(
    viewModel: ConnectivitySettingsViewModel,
    navigator: dev.drobek.geeflow.navigation.Navigator
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    ConnectivitySettingsContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent
    )

    NavigatorEffect(navigator, viewModel.navEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectivitySettingsContent(
    viewState: ConnectivitySettingsViewState,
    onEvent: (ConnectivitySettingsEvent) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    if (isWidthExpanded()) {
        CompactContent(
            viewState = viewState,
            onEvent = onEvent,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        )
    } else {
        GeeFlowScaffold(
            title = stringResource(Res.string.device_settings_connectivity),
            subtitle = stringResource(Res.string.device_settings_connectivity_description),
            navIconClick = { onEvent(CloseClicked) },
            scrollBehavior = scrollBehavior,
            content = {
                CompactContent(
                    viewState = viewState,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        )
    }
}

@Composable
private fun CompactContent(
    viewState: ConnectivitySettingsViewState,
    onEvent: (ConnectivitySettingsEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = GeeFlowTheme.spacing.contentHorizontal,
            vertical = GeeFlowTheme.spacing.contentVertical
        ),
        verticalArrangement = spacedBy(16.dp)
    ) {
        item {
            SettingsToggleRow(
                title = stringResource(Res.string.device_settings_connectivity_smart_scale),
                subtitle = stringResource(Res.string.device_settings_connectivity_smart_scale_description),
                checked = viewState.smartScaleEnabled,
                onCheckedChanged = { onEvent(SmartScaleToggled(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { HorizontalDivider() }
        itemsIndexed(viewState.scales) { _, scale ->
            ScaleItem(
                scale = scale,
                onConnectionClicked = { onEvent(ScaleConnectionClicked(scale.name)) }
            )
        }
        item {
            when {
                !viewState.smartScaleEnabled -> Text(
                    text = stringResource(Res.string.device_settings_connectivity_smart_scale_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )

                viewState.isSearching -> Text(
                    text = stringResource(Res.string.device_settings_connectivity_searching),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )

                viewState.nothingConnected -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.device_settings_connectivity_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                    TextButton(onClick = { onEvent(RescanClicked) }) {
                        Text(stringResource(Res.string.device_settings_connectivity_rescan))
                    }
                }
            }
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}


@Composable
private fun ScaleItem(
    scale: ScaleViewItem,
    onConnectionClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = scale.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        HorizontalSpacer(16.dp)
        ScaleConnectionButton(
            connectionStatus = scale.connectionStatus,
            onClick = onConnectionClicked
        )
    }
}

@Composable
private fun ScaleConnectionButton(
    connectionStatus: ScaleConnectionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        when (connectionStatus) {
            ScaleConnectionStatus.Disconnected -> Color.Transparent
            ScaleConnectionStatus.Connecting -> MaterialTheme.colorScheme.surfaceContainer
            ScaleConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        }
    )

    val contentColor by animateColorAsState(
        when (connectionStatus) {
            ScaleConnectionStatus.Disconnected -> MaterialTheme.colorScheme.primary
            ScaleConnectionStatus.Connecting -> MaterialTheme.colorScheme.onSurfaceVariant
            ScaleConnectionStatus.Connected -> MaterialTheme.colorScheme.onPrimary
        }
    )

    val text = when (connectionStatus) {
        ScaleConnectionStatus.Disconnected -> stringResource(Res.string.common_connect)
        ScaleConnectionStatus.Connecting -> stringResource(Res.string.device_dashboard_connecting)
        ScaleConnectionStatus.Connected -> stringResource(Res.string.common_disconnect)
    }

    TextButton(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (connectionStatus == ScaleConnectionStatus.Connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else if (connectionStatus == ScaleConnectionStatus.Connected) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = rememberVectorPainter(Icons.Filled.BluetoothConnected),
                contentDescription = null
            )
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDisabled() = GeeFlowTheme(false) {
    ConnectivitySettingsContent(ConnectivitySettingsViewState(smartScaleEnabled = false))
}

@Composable
@GeeFlowScreenPreview
private fun PreviewSearching() = GeeFlowTheme(false) {
    ConnectivitySettingsContent(ConnectivitySettingsViewState(smartScaleEnabled = true, scales = emptyList()))
}

@Composable
@GeeFlowScreenPreview
private fun PreviewWithScales() = GeeFlowTheme(false) {
    ConnectivitySettingsContent(
        ConnectivitySettingsViewState(
            smartScaleEnabled = true,
            scales = listOf(
                ScaleViewItem("Bookoo Themis Ultra", ScaleConnectionStatus.Disconnected),
                ScaleViewItem("Acaia Lunar", ScaleConnectionStatus.Connected)
            )
        )
    )
}
