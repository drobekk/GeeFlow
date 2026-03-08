package dev.drobek.geeflow.presentation.feature.device.list

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRemoveClicked
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceSetAsDefaultClicked
import dev.drobek.geeflow.presentation.feature.device.list.navigation.DeviceListNavigation
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.components.GeeFlowScaffold
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_favourite
import geeflow.composeapp.generated.resources.common_more
import geeflow.composeapp.generated.resources.common_remove
import geeflow.composeapp.generated.resources.device_list_screen_add_device
import geeflow.composeapp.generated.resources.device_list_screen_empty
import geeflow.composeapp.generated.resources.device_list_screen_set_as_default
import geeflow.composeapp.generated.resources.device_list_screen_subtitle
import geeflow.composeapp.generated.resources.device_list_screen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeviceListScreen(navigation: DeviceListNavigation) {
    val viewModel = koinViewModel<DeviceListViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    DevicesListContent(
        viewState = viewState,
        showBackButton = !navigation.isAtRoot,
        onEvent = viewModel::handleEvent
    )

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.AddDevice -> navigation.showAddDevice()
            is Navigation.Back -> navigation.back()
            is Navigation.DeviceDetails -> {
                navigation.clearBackStack()
                navigation.showDeviceDashboard(it.id)
            }
        }
    }
}

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

    GeeFlowScaffold(
        title = title,
        subtitle = subtitle,
        navIconPainter = navIconPainter,
        navIconClick = navIconClick,
        floatingActionButton = { AddButton(onEvent = { onEvent(DeviceListEvent.AddDeviceClicked) }) },
        content = {
            DeviceList(
                devices = viewState.devices,
                onEvent = onEvent,
                contentPadding = it
            )
        }
    )
}

@Composable
private fun DeviceList(
    devices: List<DeviceListViewState.Device>,
    onEvent: (DeviceListEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) = LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding + PaddingValues(
        horizontal = GeeFlowTheme.spacing.contentHorizontal,
        vertical = GeeFlowTheme.spacing.contentVertical
    ),
    verticalArrangement = Arrangement.spacedBy(16.dp)
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
    onEvent: (DeviceListEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = { onEvent(DeviceClicked(device)) })
            .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedVisibility(
                    visible = device.favourite,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Filled.Star),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp),
                        contentDescription = null
                    )
                }
            }
            Text(
                text = device.id,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
            modifier = Modifier
        ) {
            Icon(
                painter = rememberVectorPainter(image = Icons.Filled.MoreVert),
                contentDescription = stringResource(Res.string.common_more)
            )
        }
        DropdownMenu(
            expanded = isMenuVisible,
            onDismissRequest = { isMenuVisible = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.device_list_screen_set_as_default)) },
                leadingIcon = {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Filled.Star),
                        contentDescription = stringResource(Res.string.common_favourite)
                    )
                },
                onClick = {
                    onEvent(DeviceSetAsDefaultClicked(device))
                    isMenuVisible = false
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.common_remove)) },
                leadingIcon = {
                    Icon(
                        rememberVectorPainter(Icons.Filled.Delete),
                        contentDescription = null
                    )
                },
                onClick = {
                    onEvent(DeviceRemoveClicked(device))
                    isMenuVisible = false
                }
            )
        }
    }
}

@Composable
private fun AddButton(
    modifier: Modifier = Modifier,
    onEvent: (DeviceListEvent) -> Unit
) = FloatingActionButton(
    modifier = modifier.padding(
        horizontal = GeeFlowTheme.spacing.fabHorizontal,
        vertical = GeeFlowTheme.spacing.fabVertical
    ),
    onClick = { onEvent(DeviceListEvent.AddDeviceClicked) },
    content = {
        Icon(
            painter = rememberVectorPainter(image = Icons.Filled.Add),
            contentDescription = stringResource(Res.string.device_list_screen_add_device)
        )
    }
)

@Composable
private fun ListEmptyItem(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(Res.string.device_list_screen_empty),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    DevicesListContent(
        DeviceListViewState(
            devices = listOf(
                DeviceListViewState.Device(
                    name = "Data-S",
                    id = "B0234556",
                    favourite = true
                ),
                DeviceListViewState.Device(
                    name = "Data-S",
                    id = "B02345556",
                    favourite = false
                )
            )
        )
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    DevicesListContent(DeviceListViewState())
}
