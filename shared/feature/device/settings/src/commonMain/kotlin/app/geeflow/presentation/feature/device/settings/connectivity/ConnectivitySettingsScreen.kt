package app.geeflow.presentation.feature.device.settings.connectivity

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.CloseClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.RescanClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.ScaleConnectionClicked
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsEvent.SmartScaleToggled
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.ScaleConnectionStatus
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsViewState.ScaleViewItem
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowDetailScaffold
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_connect
import geeflow.shared.core.ui.generated.resources.common_disconnect
import geeflow.shared.core.ui.generated.resources.common_ok
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_dashboard_connecting
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_not_found
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_rescan
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_searching
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_smart_scale
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_smart_scale_description
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity_smart_scale_info
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun ConnectivitySettingsScreen(
    viewModel: ConnectivitySettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val acknowledge = stringResource(CoreRes.string.common_ok)

    ConnectivitySettingsContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent,
    )

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is ShowSnackbar -> coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = it.message,
                    actionLabel = acknowledge,
                    duration = SnackbarDuration.Indefinite,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectivitySettingsContent(
    viewState: ConnectivitySettingsViewState,
    onEvent: (ConnectivitySettingsEvent) -> Unit = {},
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHost: @Composable () -> Unit = { SnackbarHost(snackbarHostState) }

    GeeFlowDetailScaffold(
        title = stringResource(Res.string.device_settings_connectivity),
        subtitle = stringResource(Res.string.device_settings_connectivity_description),
        navIconClick = { onEvent(CloseClicked) },
        scrollBehavior = scrollBehavior,
        snackbarHost = snackbarHost,
        content = {
            CompactContent(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            )
        },
        modifier = Modifier,
    )
}

@Composable
private fun CompactContent(
    viewState: ConnectivitySettingsViewState,
    onEvent: (ConnectivitySettingsEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = GeeFlowTheme.spacing.contentHorizontal,
            vertical = GeeFlowTheme.spacing.contentVertical,
        ),
        verticalArrangement = spacedBy(16.dp),
    ) {
        item {
            GeeFlowToggleListItem(
                title = stringResource(Res.string.device_settings_connectivity_smart_scale),
                subtitle = stringResource(Res.string.device_settings_connectivity_smart_scale_description),
                checked = viewState.smartScaleEnabled,
                onCheckedChanged = { onEvent(SmartScaleToggled(it)) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(),
            )
        }
        item { HorizontalDivider() }
        itemsIndexed(viewState.scales) { _, scale ->
            ScaleItem(
                scale = scale,
                onConnectionClicked = { onEvent(ScaleConnectionClicked(scale.name)) },
            )
        }
        item {
            when {
                !viewState.smartScaleEnabled -> Text(
                    text = stringResource(Res.string.device_settings_connectivity_smart_scale_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )

                viewState.isSearching -> Text(
                    text = stringResource(Res.string.device_settings_connectivity_searching),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )

                viewState.nothingConnected -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.device_settings_connectivity_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                    )
                    TextButton(onClick = { onEvent(RescanClicked) }) {
                        Text(stringResource(Res.string.device_settings_connectivity_rescan))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScaleItem(
    scale: ScaleViewItem,
    onConnectionClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = scale.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        HorizontalSpacer(16.dp)
        ScaleConnectionButton(
            connectionStatus = scale.connectionStatus,
            onClick = onConnectionClicked,
        )
    }
}

@Composable
private fun ScaleConnectionButton(
    connectionStatus: ScaleConnectionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        when (connectionStatus) {
            ScaleConnectionStatus.Disconnected -> Color.Transparent
            ScaleConnectionStatus.Connecting -> MaterialTheme.colorScheme.surfaceContainer
            ScaleConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        },
    )

    val contentColor by animateColorAsState(
        when (connectionStatus) {
            ScaleConnectionStatus.Disconnected -> MaterialTheme.colorScheme.primary
            ScaleConnectionStatus.Connecting -> MaterialTheme.colorScheme.onSurfaceVariant
            ScaleConnectionStatus.Connected -> MaterialTheme.colorScheme.onPrimary
        },
    )

    val text = when (connectionStatus) {
        ScaleConnectionStatus.Disconnected -> stringResource(CoreRes.string.common_connect)
        ScaleConnectionStatus.Connecting -> stringResource(Res.string.device_dashboard_connecting)
        ScaleConnectionStatus.Connected -> stringResource(CoreRes.string.common_disconnect)
    }

    TextButton(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        if (connectionStatus == ScaleConnectionStatus.Connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else if (connectionStatus == ScaleConnectionStatus.Connected) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = rememberVectorPainter(Icons.Filled.BluetoothConnected),
                contentDescription = null,
            )
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    ConnectivitySettingsContent(ConnectivitySettingsViewState(smartScaleEnabled = false))
}
