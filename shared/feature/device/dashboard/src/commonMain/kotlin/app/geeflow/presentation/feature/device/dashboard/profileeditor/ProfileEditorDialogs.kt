package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DetailsConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DialogDismissed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetConfirmed
import app.geeflow.ui.components.DefaultControlWidth
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowInputPad
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.unit_grams
import geeflow.shared.core.ui.generated.resources.unit_milliliters
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_description
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_name
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_title
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_title_volume
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_title_weight
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun ProfileEditorDialogs(
    dialog: ProfileEditorDialog,
    viewState: ProfileEditorViewState,
    onEvent: (ProfileEditorEvent) -> Unit,
) {
    val onDismiss = { onEvent(DialogDismissed) }
    when (dialog) {
        is ProfileEditorDialog.Details -> ProfileDetailsDialog(
            currentName = viewState.profileName,
            currentDescription = viewState.description,
            onConfirm = { name, description -> onEvent(DetailsConfirmed(name, description)) },
            onDismiss = onDismiss,
        )

        is ProfileEditorDialog.FinishTargetValue -> FinishTargetDialog(
            dialog = dialog,
            onConfirm = { onEvent(FinishTargetConfirmed(it)) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ProfileDetailsDialog(
    currentName: String,
    currentDescription: String,
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val nameState = rememberTextFieldState(initialText = currentName)
    val descriptionState = rememberTextFieldState(initialText = currentDescription)
    EditorDialog(
        onDismiss = onDismiss,
        title = { GeeFlowDialogTopBar(stringResource(Res.string.profile_editor_details_title), onDismiss) },
    ) {
        GeeFlowOutlinedTextField(
            state = nameState,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.profile_editor_details_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(12.dp)
        GeeFlowOutlinedTextField(
            state = descriptionState,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.profile_editor_details_description)) },
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(24.dp)
        ConfirmButton(enabled = nameState.text.isNotBlank()) {
            onConfirm(nameState.text.toString(), descriptionState.text.toString())
        }
    }
}

@Composable
private fun FinishTargetDialog(
    dialog: ProfileEditorDialog.FinishTargetValue,
    onConfirm: (target: Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val targetRange = MinFinishTarget..ProfileEditorDefaults.MaxFinishTarget

    val title = when (dialog.type) {
        FinishTargetType.Volume -> Res.string.profile_editor_finish_title_volume
        FinishTargetType.Weight -> Res.string.profile_editor_finish_title_weight
    }
    val unit = stringResource(
        when (dialog.type) {
            FinishTargetType.Volume -> CoreRes.string.unit_milliliters
            FinishTargetType.Weight -> CoreRes.string.unit_grams
        },
    )

    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            VerticalSpacer(16.dp)
            GeeFlowInputPad(
                valueRange = targetRange,
                unit = unit,
                color = dialog.type.color(),
                allowDecimal = false,
                onConfirm = onConfirm,
                onBack = onDismiss,
            )
        }
    }
}

/** The editor's dialog frame: closing from the top bar is what cancels, so there is no cancel button. */
@Composable
private fun EditorDialog(
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    content: @Composable ColumnScope.() -> Unit,
) {
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp)
                .then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            title()
            VerticalSpacer(24.dp)
            content()
        }
    }
}

@Composable
private fun ColumnScope.ConfirmButton(enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.align(Alignment.End)) {
        Text(stringResource(CoreRes.string.common_confirm))
    }
}

private const val MinFinishTarget = 1f
private val SinglePickerContentWidth = 208.dp
private val DualPickerContentWidth = DefaultControlWidth * 2 + 16.dp
