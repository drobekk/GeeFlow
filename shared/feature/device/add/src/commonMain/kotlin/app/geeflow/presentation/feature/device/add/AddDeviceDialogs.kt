package app.geeflow.presentation.feature.device.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceManufacturer
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.Dialog.ExperimentalWarning
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.modifier.verticalScrollWithFade
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.feature.device.add.generated.resources.Res
import geeflow.shared.feature.device.add.generated.resources.add_device_warning_accept
import geeflow.shared.feature.device.add.generated.resources.add_device_warning_cancel
import geeflow.shared.feature.device.add.generated.resources.add_device_warning_message
import geeflow.shared.feature.device.add.generated.resources.add_device_warning_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AddDeviceDialogs(
    dialog: AddDeviceViewState.Dialog?,
    onEvent: (AddDeviceEvent) -> Unit,
) {
    when (dialog) {
        is ExperimentalWarning -> ExperimentalWarningDialog(onEvent = onEvent)
        null -> {}
    }
}

@Composable
private fun ExperimentalWarningDialog(
    onEvent: (AddDeviceEvent) -> Unit,
) {
    GeeFlowDialog(
        onDismissRequest = { onEvent(AddDeviceEvent.CancelWarningClicked) },
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.add_device_warning_title),
                onCloseClick = { onEvent(AddDeviceEvent.CancelWarningClicked) },
            )
            VerticalSpacer(16.dp)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScrollWithFade(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(Res.string.add_device_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            VerticalSpacer(24.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = { onEvent(AddDeviceEvent.CancelWarningClicked) }) {
                    Text(text = stringResource(Res.string.add_device_warning_cancel))
                }
                Button(onClick = { onEvent(AddDeviceEvent.AcceptWarningClicked) }) {
                    Text(text = stringResource(Res.string.add_device_warning_accept))
                }
            }
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    AddDeviceDialogs(
        dialog = ExperimentalWarning(
            device = Device(
                id = 0L,
                name = "Wendougee",
                connection = DeviceConnection.Ble("mac", "mac"),
                manufacturer = DeviceManufacturer.WENDOUGEE,
                model = "model",
                version = "1.0",
            ),
        ),
        onEvent = {},
    )
}
