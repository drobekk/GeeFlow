package app.geeflow.presentation.feature.device.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.presentation.feature.device.list.DeviceListEvent.BackClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRemoveClicked
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceSetAsDefaultClicked
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.modifier.geeFlowInsetsEndPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.common_favourite
import geeflow.shared.core.ui.generated.resources.common_more
import geeflow.shared.core.ui.generated.resources.common_remove
import geeflow.shared.feature.device.list.generated.resources.Res
import geeflow.shared.feature.device.list.generated.resources.device_list_screen_add_device
import geeflow.shared.feature.device.list.generated.resources.device_list_screen_empty
import geeflow.shared.feature.device.list.generated.resources.device_list_screen_set_as_default
import geeflow.shared.feature.device.list.generated.resources.device_list_screen_subtitle
import geeflow.shared.feature.device.list.generated.resources.device_list_screen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
fun DeviceListScreen(navigator: Navigator) {
    val viewModel = koinViewModel<DeviceListViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val prevDest = navigator.getPreviousDestination()
    val showBackButton = !navigator.isAtRoot && when (prevDest) {
        is DeviceDashboard -> viewState.devices.any { it.id == prevDest.deviceId }
        else -> true
    }

    DevicesListContent(
        viewState = viewState,
        showBackButton = showBackButton,
        onEvent = viewModel::handleEvent,
    )

    NavigatorEffect(navigator, viewModel.navEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicesListContent(
    viewState: DeviceListViewState,
    showBackButton: Boolean = true,
    onEvent: (DeviceListEvent) -> Unit = {},
) {
    val title = stringResource(Res.string.device_list_screen_title)
    val subtitle = stringResource(Res.string.device_list_screen_subtitle)
    val navIconPainter = if (showBackButton) rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack) else null
    val navIconClick = { onEvent(BackClicked) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    GeeFlowScaffold(
        title = title,
        subtitle = subtitle,
        scrollBehavior = scrollBehavior,
        navIconPainter = navIconPainter,
        navIconClick = navIconClick,
        floatingActionButton = { AddButton(onEvent = { onEvent(DeviceListEvent.AddDeviceClicked) }) },
        content = {
            DeviceList(
                devices = viewState.devices,
                onEvent = onEvent,
                contentPadding = it,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            )
        },
    )
}

@Composable
private fun DeviceList(
    devices: List<DeviceListViewState.Device>,
    onEvent: (DeviceListEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) = LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding + PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = GeeFlowTheme.spacing.contentVertical,
    ),
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    if (devices.isEmpty()) {
        item { ListEmptyItem(modifier = Modifier.fillParentMaxSize()) }
    }
    items(devices) { DeviceItem(device = it, onEvent = onEvent) }
    item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
}

@Composable
private fun DeviceItem(
    device: DeviceListViewState.Device,
    onEvent: (DeviceListEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = { onEvent(DeviceClicked(device)) })
            .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                AnimatedVisibility(
                    visible = device.favourite,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Filled.Star),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                        contentDescription = null,
                    )
                }
            }
            if (device.macAddress.isNotEmpty()) {
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DeviceItemMenu(device, onEvent)
    }
}

@Composable
private fun DeviceItemMenu(device: DeviceListViewState.Device, onEvent: (DeviceListEvent) -> Unit) {
    var isMenuVisible by remember(device) { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isMenuVisible = true },
            modifier = Modifier,
        ) {
            Icon(
                painter = rememberVectorPainter(image = Icons.Filled.MoreVert),
                contentDescription = stringResource(CoreRes.string.common_more),
            )
        }
        DropdownMenu(
            expanded = isMenuVisible,
            onDismissRequest = { isMenuVisible = false },
            shape = MaterialTheme.shapes.large,
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.device_list_screen_set_as_default)) },
                leadingIcon = {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Filled.Star),
                        contentDescription = stringResource(CoreRes.string.common_favourite),
                    )
                },
                onClick = {
                    onEvent(DeviceSetAsDefaultClicked(device))
                    isMenuVisible = false
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(CoreRes.string.common_remove)) },
                leadingIcon = {
                    Icon(
                        rememberVectorPainter(Icons.Filled.Delete),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onEvent(DeviceRemoveClicked(device))
                    isMenuVisible = false
                },
            )
        }
    }
}

@Composable
private fun AddButton(
    modifier: Modifier = Modifier,
    onEvent: (DeviceListEvent) -> Unit,
) = FloatingActionButton(
    modifier = modifier
        .padding(
            horizontal = GeeFlowTheme.spacing.fabHorizontal,
            vertical = GeeFlowTheme.spacing.fabVertical,
        )
        .geeFlowInsetsEndPadding(),
    onClick = { onEvent(DeviceListEvent.AddDeviceClicked) },
    content = {
        Icon(
            painter = rememberVectorPainter(image = Icons.Filled.Add),
            contentDescription = stringResource(Res.string.device_list_screen_add_device),
        )
    },
)

@Composable
private fun ListEmptyItem(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.device_list_screen_empty),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    DevicesListContent(
        DeviceListViewState(
            devices = listOf(
                DeviceListViewState.Device(
                    name = "Data-S",
                    id = 1L,
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    favourite = true,
                ),
                DeviceListViewState.Device(
                    name = "Data-S",
                    id = 2L,
                    macAddress = "",
                    favourite = false,
                ),
            ),
        ),
    )
}
