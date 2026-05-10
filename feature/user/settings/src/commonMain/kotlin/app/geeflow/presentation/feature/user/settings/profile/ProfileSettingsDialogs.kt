package app.geeflow.presentation.feature.user.settings.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DeleteConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RemovePhotoConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsEvent.RenameConfirmed
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsViewState.Dialog
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.VerticalSpacer
import geeflow.core.ui.generated.resources.common_confirm
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_profile_delete
import geeflow.feature.user.settings.generated.resources.user_settings_profile_delete_confirmation
import geeflow.feature.user.settings.generated.resources.user_settings_profile_remove_picture
import geeflow.feature.user.settings.generated.resources.user_settings_profile_remove_picture_confirmation
import geeflow.feature.user.settings.generated.resources.user_settings_profile_rename_name_hint
import geeflow.feature.user.settings.generated.resources.user_settings_profile_rename_title
import org.jetbrains.compose.resources.stringResource
import geeflow.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun ProfileSettingsDialogs(
    dialog: Dialog?,
    currentName: String,
    onEvent: (ProfileSettingsEvent) -> Unit,
) {
    when (dialog) {
        Dialog.Rename -> RenameDialog(
            currentName = currentName,
            onConfirm = { onEvent(RenameConfirmed(it)) },
            onDismiss = { onEvent(DialogDismissed) },
        )
        Dialog.RemovePhoto -> RemovePhotoDialog(
            onConfirm = { onEvent(RemovePhotoConfirmed) },
            onDismiss = { onEvent(DialogDismissed) },
        )
        Dialog.Delete -> DeleteDialog(
            onConfirm = { onEvent(DeleteConfirmed) },
            onDismiss = { onEvent(DialogDismissed) },
        )
        null -> {}
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val nameState = rememberTextFieldState(initialText = currentName)
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.user_settings_profile_rename_title),
                onCloseClick = onDismiss,
            )
            VerticalSpacer(16.dp)
            GeeFlowOutlinedTextField(
                state = nameState,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text(stringResource(Res.string.user_settings_profile_rename_name_hint)) },
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

@Composable
private fun RemovePhotoDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.user_settings_profile_remove_picture),
                onCloseClick = onDismiss,
            )
            VerticalSpacer(24.dp)
            Text(stringResource(Res.string.user_settings_profile_remove_picture_confirmation))
            VerticalSpacer(24.dp)
            Button(
                onClick = onConfirm,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        }
    }
}

@Composable
private fun DeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.user_settings_profile_delete),
                onCloseClick = onDismiss,
            )
            VerticalSpacer(24.dp)
            Text(stringResource(Res.string.user_settings_profile_delete_confirmation))
            VerticalSpacer(24.dp)
            Button(
                onClick = onConfirm,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        }
    }
}
