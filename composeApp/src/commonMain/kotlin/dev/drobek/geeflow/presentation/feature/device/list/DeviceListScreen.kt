package dev.drobek.geeflow.presentation.feature.device.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.DeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.list.DeviceListEvent.BackClicked
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.components.AdaptiveColumnRow
import dev.drobek.geeflow.ui.components.GeeFlowTopBar
import dev.drobek.geeflow.ui.theme.GeeFlowPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.devices_list_screen_add_device
import geeflow.composeapp.generated.resources.devices_list_screen_subtitle
import geeflow.composeapp.generated.resources.devices_list_screen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeviceListScreen(navigation: DeviceNavigation) {
    val viewModel = koinViewModel<DeviceListViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    DevicesListContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent
    )

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.AddDevice -> navigation.showAddDevice()
            is Navigation.Back -> navigation.back()
            is Navigation.DeviceDetails -> navigation.showDeviceDetails(it.id)
        }
    }
}

@Composable
private fun DevicesListContent(
    viewState: DeviceListViewState,
    onEvent: (DeviceListEvent) -> Unit = {},
) {
    AdaptiveColumnRow(
        modifier = Modifier,
        first = {
            GeeFlowTopBar(
                title = stringResource(Res.string.devices_list_screen_title),
                subtitle = stringResource(Res.string.devices_list_screen_subtitle),
                navIconClick = { onEvent(BackClicked) }
            )
        },
        second = {
            DeviceList(
                devices = viewState.devices,
                onEvent = onEvent
            )
        }
    )
}

@Composable
private fun DeviceList(
    devices: List<DeviceListViewState.Device>,
    onEvent: (DeviceListEvent) -> Unit
) {
    Column {
        devices.forEach {
            Text(
                text = it.id,
                modifier = Modifier
                    .padding(32.dp)
            )
        }
        Button(onClick = { onEvent(DeviceListEvent.AddDeviceClicked) }) {
            Text(text = stringResource(Res.string.devices_list_screen_add_device))
        }
    }
}

@Composable
@GeeFlowPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    DevicesListContent(
        DeviceListViewState(
            devices = listOf(
                DeviceListViewState.Device(
                    name = "Data-S",
                    id = "B0234556"
                )
            )
        )
    )
}

@Composable
@GeeFlowPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    DevicesListContent(DeviceListViewState())
}
