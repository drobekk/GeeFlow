package dev.drobek.geeflow.presentation.feature.device.add

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.DeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.NearbyDeviceClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.QrCodeScanned
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowNearbyDevicesClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowQrCodeScannerClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewState.Method.NearbyDevices
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewState.Method.QrCodeScanner
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.VerticalSpacer
import dev.drobek.geeflow.ui.components.AdaptiveColumnRow
import dev.drobek.geeflow.ui.components.GeeFlowTopBar
import dev.drobek.geeflow.ui.isExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.isPreview
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.add_device_screen_description
import geeflow.composeapp.generated.resources.add_device_screen_empty
import geeflow.composeapp.generated.resources.add_device_screen_no_camera_permission
import geeflow.composeapp.generated.resources.add_device_screen_scan_qr
import geeflow.composeapp.generated.resources.add_device_screen_show_nearby
import geeflow.composeapp.generated.resources.add_device_screen_title
import geeflow.composeapp.generated.resources.common_open_settings
import geeflow.composeapp.generated.resources.device_dashboard_no_bt_permission
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
internal fun AddDeviceScreen(
    viewModel: AddDeviceViewModel,
    deviceNavigation: DeviceNavigation
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AddDeviceContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent
    )

    LifecycleResumeEffect(Unit) {
        viewModel.handleEvent(AddDeviceEvent.Resumed)
        onPauseOrDispose {}
    }

    EventsDispatcher(viewModel.events) {
        when (it) {
            is Navigation.Back -> deviceNavigation.back()
            is Navigation.DevicesList -> {
                deviceNavigation.clearBackStack()
                deviceNavigation.showDevicesList()
            }

            is ShowSnackbar -> coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(it.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceContent(
    viewState: AddDeviceViewState,
    onEvent: (AddDeviceEvent) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Box {
        AdaptiveColumnRow(
            first = {
                GeeFlowTopBar(
                    title = stringResource(Res.string.add_device_screen_title),
                    subtitle = stringResource(Res.string.add_device_screen_description),
                    navIconClick = { onEvent(BackClicked) }
                )
            },
            second = { Content(viewState, onEvent) },
            firstAlignment = Alignment.TopStart,
            secondAlignment = Alignment.TopStart
        )
        SnackbarHost(
            modifier = Modifier
                .navigationBarsPadding()
                .align(Alignment.BottomCenter),
            hostState = snackbarHostState
        )
    }
}

@Composable
private fun Content(
    viewState: AddDeviceViewState,
    onEvent: (AddDeviceEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewState.method,
            modifier = Modifier.navigationBarsPadding()
        ) { method ->
            when (method) {
                is NearbyDevices -> NearbyDevicesList(
                    model = method,
                    onEvent = onEvent
                )

                is QrCodeScanner -> Scanner(
                    model = method,
                    onEvent = onEvent
                )
            }
        }
        if (viewState.method.changeMethodButtonVisible) {
            FloatingActionButton(
                onClick = {
                    when (viewState.method) {
                        is NearbyDevices -> onEvent(ShowQrCodeScannerClicked)
                        is QrCodeScanner -> onEvent(ShowNearbyDevicesClicked)
                    }
                },
                modifier = Modifier
                    .padding(bottom = 36.dp)
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(
                        when (viewState.method) {
                            is NearbyDevices -> Res.string.add_device_screen_scan_qr
                            is QrCodeScanner -> Res.string.add_device_screen_show_nearby
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NearbyDevicesList(
    model: NearbyDevices,
    onEvent: (AddDeviceEvent) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = if (isExpanded()) 48.dp else 32.dp,
                vertical = if (isExpanded()) 24.dp else 60.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(model.devices) {
                DeviceItem(it, onEvent)
            }
        }

        when {
            model.showMissingPermissionMessage -> MissingPermissionsMessage(onEvent)
            model.devices.isEmpty() -> EmptyListMessage()
        }
    }
}

@Composable
private fun DeviceItem(
    model: AddDeviceViewState.DeviceItem,
    onEvent: (AddDeviceEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = { onEvent(NearbyDeviceClicked(model.id)) })
            .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
    ) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = model.id,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun Scanner(
    model: QrCodeScanner,
    onEvent: (AddDeviceEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val shape = RoundedCornerShape(32.dp)
        val modifier = Modifier
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clip(shape)
            .padding(2.dp)
            .background(Color.Black, shape)
            .fillMaxSize()
            .weight(1f)

        if (isPreview) {
            Box(modifier = modifier)
        } else {
            ScannerWithPermissions(
                modifier = modifier.clipToBounds(),
                onScanned = {
                    onEvent(QrCodeScanned(it))
                    !model.scanningEnabled
                },
                types = listOf(CodeType.QR),
                cameraPosition = CameraPosition.BACK,
                enableTorch = false,
                permissionDeniedContent = { permissionState ->
                    Column(
                        modifier = modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VerticalSpacer(1f)
                        Text(
                            modifier = Modifier.padding(6.dp),
                            text = stringResource(Res.string.add_device_screen_no_camera_permission),
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                        Button(onClick = { permissionState.goToSettings() }) {
                            Text(text = stringResource(Res.string.common_open_settings))
                        }
                        VerticalSpacer(1f)
                    }
                }
            )
        }
        VerticalSpacer(32.dp)
    }
}

@Composable
private fun MissingPermissionsMessage(
    onEvent: (AddDeviceEvent) -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.device_dashboard_no_bt_permission),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,

            )
        VerticalSpacer(24.dp)
        OutlinedButton(
            onClick = { onEvent(AddDeviceEvent.OpenSystemSettingsClicked) }
        ) {
            Text(text = stringResource(Res.string.common_open_settings))
        }
    }
}

@Composable
private fun EmptyListMessage(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(Res.string.add_device_screen_empty),
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    AddDeviceContent(
        viewState = AddDeviceViewState(
            method = NearbyDevices(
                changeMethodButtonVisible = true,
                devices = listOf(
                    AddDeviceViewState.DeviceItem(
                        id = "B0234556",
                        name = "DATA-S"
                    )
                )
            )
        )
    )
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    AddDeviceContent(
        viewState = AddDeviceViewState(
            method = NearbyDevices()
        )
    )
}
