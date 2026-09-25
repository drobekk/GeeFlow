package app.geeflow.presentation.feature.device.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DeviceRenameConfirmed
import app.geeflow.presentation.feature.device.list.DeviceListEvent.DialogDismissed
import app.geeflow.presentation.feature.device.list.DeviceListViewState.Dialog
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.feature.device.list.generated.resources.Res
import geeflow.shared.feature.device.list.generated.resources.device_list_screen_rename_name_hint
import geeflow.shared.feature.device.list.generated.resources.device_list_screen_rename_title
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun DeviceListDialogs(
    dialog: Dialog?,
    onEvent: (DeviceListEvent) -> Unit,
) {
    when (dialog) {
        is Dialog.Rename -> RenameDialog(
            device = dialog.device,
            onConfirm = { onEvent(DeviceRenameConfirmed(dialog.device, it)) },
            onDismiss = { onEvent(DialogDismissed) },
        )
        null -> {}
    }
}

@Composable
private fun RenameDialog(
    device: DeviceListViewState.Device,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val nameState = rememberTextFieldState(initialText = device.name)
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.device_list_screen_rename_title),
                onCloseClick = onDismiss,
            )
            VerticalSpacer(16.dp)
            GeeFlowOutlinedTextField(
                state = nameState,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(stringResource(Res.string.device_list_screen_rename_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(24.dp)
            Button(
                onClick = { onConfirm(nameState.text.toString()) },
                enabled = nameState.text.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        }
    }
}
