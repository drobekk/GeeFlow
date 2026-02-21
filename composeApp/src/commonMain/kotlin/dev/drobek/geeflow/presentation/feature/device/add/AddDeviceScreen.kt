package dev.drobek.geeflow.presentation.feature.device.add

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.drobek.geeflow.presentation.feature.device.DeviceNavigation
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.FormSubmitted
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.QrCodeScanned
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowFormClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowQrCodeScannerClicked
import dev.drobek.geeflow.presentation.feature.device.add.AddDeviceViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.ui.EventsDispatcher
import dev.drobek.geeflow.ui.VerticalSpacer
import dev.drobek.geeflow.ui.components.AdaptiveColumnRow
import dev.drobek.geeflow.ui.components.GeeFlowIconButton
import dev.drobek.geeflow.ui.components.GeeFlowOutlinedTextField
import dev.drobek.geeflow.ui.components.GeeFlowTopBar
import dev.drobek.geeflow.ui.isExpanded
import dev.drobek.geeflow.ui.theme.GeeFlowScreenPreview
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import dev.drobek.geeflow.ui.theme.isPreview
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.add_device_screen_description
import geeflow.composeapp.generated.resources.add_device_screen_enter_manually
import geeflow.composeapp.generated.resources.add_device_screen_no_camera_permission
import geeflow.composeapp.generated.resources.add_device_screen_scan_qr
import geeflow.composeapp.generated.resources.add_device_screen_title
import geeflow.composeapp.generated.resources.common_confirm
import geeflow.composeapp.generated.resources.common_device_name
import geeflow.composeapp.generated.resources.common_mac_address
import geeflow.composeapp.generated.resources.common_open_settings
import geeflow.composeapp.generated.resources.common_serial_number
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
internal fun AddDeviceScreen(deviceNavigation: DeviceNavigation) {
    val viewModel = koinViewModel<AddDeviceViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    AddDeviceContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent
    )

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
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .height(IntrinsicSize.Min),
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
    AnimatedContent(
        targetState = viewState.isFormVisible,
        modifier = Modifier.navigationBarsPadding()
    ) { form ->
        if (form) {
            Form(
                showQrCodeScannerButtonVisible = viewState.isQrCodeScannerButtonVisible,
                onEvent = onEvent
            )
        } else {
            Scanner(
                qrCodeScanningEnabled = viewState.qrCodeScanningEnabled,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun Form(
    showQrCodeScannerButtonVisible: Boolean,
    onEvent: (AddDeviceEvent) -> Unit
) {
    val nameTextFieldState = rememberTextFieldState()
    val macAddressTextFieldState = rememberTextFieldState()
    val serialNumberTextFieldState = rememberTextFieldState()
    val buttonEnabled = nameTextFieldState.text.isNotEmpty() &&
            macAddressTextFieldState.text.isNotEmpty() &&
            serialNumberTextFieldState.text.isNotEmpty()

    fun submitForm() {
        if (buttonEnabled) {
            onEvent(
                FormSubmitted(
                    nameTextFieldState.text.toString(),
                    macAddressTextFieldState.text.toString(),
                    serialNumberTextFieldState.text.toString()
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth()
            .padding(if (isExpanded()) 24.dp else 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GeeFlowOutlinedTextField(
            state = nameTextFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.common_device_name)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )
        VerticalSpacer(16.dp)
        GeeFlowOutlinedTextField(
            state = serialNumberTextFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.common_serial_number)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )
        VerticalSpacer(16.dp)
        GeeFlowOutlinedTextField(
            state = macAddressTextFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.common_mac_address)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = if (buttonEnabled) ImeAction.Done else ImeAction.None),
            onKeyboardAction = { submitForm() }
        )
        if (showQrCodeScannerButtonVisible) {
            VerticalSpacer(16.dp)
            TextButton(
                onClick = { onEvent(ShowQrCodeScannerClicked) },
                content = {
                    Text(
                        text = stringResource(Res.string.add_device_screen_scan_qr),
                        textDecoration = TextDecoration.Underline
                    )
                }
            )
        }
        VerticalSpacer(24.dp)
        VerticalSpacer(1f)
        GeeFlowIconButton(
            painter = rememberVectorPainter(Icons.Filled.Check),
            enabled = buttonEnabled,
            contentDescription = stringResource(Res.string.common_confirm),
            onClick = { submitForm() }
        )
        VerticalSpacer(24.dp)
    }
}

@Composable
private fun Scanner(
    qrCodeScanningEnabled: Boolean = true,
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
                    !qrCodeScanningEnabled
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
        TextButton(
            onClick = { onEvent(ShowFormClicked) },
            content = {
                Text(
                    text = stringResource(Res.string.add_device_screen_enter_manually),
                    textDecoration = TextDecoration.Underline
                )
            }
        )
    }
}

@Composable
@GeeFlowScreenPreview
private fun PreviewLight() = GeeFlowTheme(false) {
    AddDeviceContent(viewState = AddDeviceViewState(isFormVisible = true))
}

@Composable
@GeeFlowScreenPreview
private fun PreviewDark() = GeeFlowTheme(true) {
    AddDeviceContent(viewState = AddDeviceViewState())
}
