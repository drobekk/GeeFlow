package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.data.brew.model.SingleDoseSettings
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DetailsConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DialogDismissed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetConfirmed
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowInputPad
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.components.GeeFlowValueListItem
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.core.ui.generated.resources.common_apply
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_save
import geeflow.shared.core.ui.generated.resources.unit_grams
import geeflow.shared.core.ui.generated.resources.unit_milliliters
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_description
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_name
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_title
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_title_volume
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_title_weight
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_grinding_size
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_grinding_speed
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_single_dose_title
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_single_dose_toggle
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_single_dose_toggle_description
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

        is ProfileEditorDialog.SingleDose -> SingleDoseDialog(dialog, viewState, onEvent, onDismiss)
    }
}

@Composable
private fun SingleDoseDialog(
    dialog: ProfileEditorDialog.SingleDose,
    viewState: ProfileEditorViewState,
    onEvent: (ProfileEditorEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val settings = dialog.settings
    EditorDialog(
        onDismiss = onDismiss,
        title = { GeeFlowDialogTopBar(stringResource(Res.string.profile_editor_single_dose_title), onDismiss) },
    ) {
        GeeFlowToggleListItem(
            title = stringResource(Res.string.profile_editor_single_dose_toggle),
            subtitle = stringResource(Res.string.profile_editor_single_dose_toggle_description),
            checked = settings.enabled,
            onCheckedChanged = { onEvent(ProfileEditorEvent.SingleDoseToggled(it)) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
        )
        VerticalSpacer(12.dp)
        GeeFlowValueListItem(
            title = stringResource(Res.string.profile_editor_grinding_size),
            subtitle = "0–600 µm",
            value = "${settings.grindingSize} µm",
            onValueConfirmed = { value ->
                value.toIntOrNull()?.let { size ->
                    onEvent(ProfileEditorEvent.GrindingSizeChanged(size))
                }
            },
            enabled = settings.enabled,
            valueRange = SingleDoseSettings.MIN_GRINDING_SIZE.toFloat()..SingleDoseSettings.MAX_GRINDING_SIZE.toFloat(),
            unit = "µm",
            allowDecimal = false,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
        )
        GeeFlowValueListItem(
            title = stringResource(Res.string.profile_editor_grinding_speed),
            subtitle = "200–1000 rpm",
            value = "${settings.grindingSpeed} rpm",
            onValueConfirmed = { value ->
                value.toIntOrNull()?.let { speed ->
                    onEvent(ProfileEditorEvent.GrindingSpeedChanged(speed))
                }
            },
            enabled = settings.enabled,
            valueRange = SingleDoseSettings.MIN_GRINDING_SPEED.toFloat()..SingleDoseSettings.MAX_GRINDING_SPEED.toFloat(),
            unit = "rpm",
            allowDecimal = false,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
        )
        VerticalSpacer(20.dp)
        Row(modifier = Modifier.align(Alignment.End)) {
            TextButton(
                onClick = { onEvent(ProfileEditorEvent.SetGrinderClicked) },
                enabled = settings.enabled && viewState.singleDoseGrinderReady,
            ) {
                Text(stringResource(CoreRes.string.common_apply))
            }
            HorizontalSpacer(24.dp)
            Button(onClick = { onEvent(ProfileEditorEvent.SingleDoseSaved) }) {
                Text(stringResource(CoreRes.string.common_save))
            }
        }
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
