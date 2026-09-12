package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DetailsConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DialogDismissed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepTypeSelected
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepValuesConfirmed
import app.geeflow.ui.components.DefaultControlWidth
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowInfinitePicker
import app.geeflow.ui.components.GeeFlowInputPad
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.common_time
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.core.ui.generated.resources.unit_grams
import geeflow.shared.core.ui.generated.resources.unit_milliliters
import geeflow.shared.core.ui.generated.resources.unit_milliliters_per_second
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_description
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_name
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_details_title
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_title_volume
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_finish_title_weight
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_step_type_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun ProfileEditorDialogs(
    dialog: ProfileEditorDialog,
    viewState: ProfileEditorViewState,
    onEvent: (ProfileEditorEvent) -> Unit,
) {
    val onDismiss = { onEvent(DialogDismissed) }
    when (dialog) {
        is ProfileEditorDialog.Advanced -> AdvancedPhaseDialog(dialog.step, onEvent)
        is ProfileEditorDialog.Details -> ProfileDetailsDialog(
            currentName = viewState.profileName,
            currentDescription = viewState.description,
            onConfirm = { name, description -> onEvent(DetailsConfirmed(name, description)) },
            onDismiss = onDismiss,
        )

        is ProfileEditorDialog.StepTypePicker -> StepTypeDialog(
            onTypeSelected = { onEvent(StepTypeSelected(it)) },
            onDismiss = onDismiss,
        )

        is ProfileEditorDialog.StepValues -> StepValuesDialog(
            dialog = dialog,
            valueRange = if (dialog.type == StepType.Pressure) viewState.pressureRange else viewState.flowRange,
            onConfirm = { time, value -> onEvent(StepValuesConfirmed(time, value)) },
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
private fun StepTypeDialog(
    onTypeSelected: (StepType) -> Unit,
    onDismiss: () -> Unit,
) {
    EditorDialog(
        onDismiss = onDismiss,
        title = { GeeFlowDialogTopBar(stringResource(Res.string.profile_editor_step_type_title), onDismiss) },
    ) {
        StepType.entries.forEach { type ->
            OutlinedButton(
                onClick = { onTypeSelected(type) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                StepTypeIcon(type)
                HorizontalSpacer(12.dp)
                Text(text = type.label(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StepValuesDialog(
    dialog: ProfileEditorDialog.StepValues,
    valueRange: ClosedFloatingPointRange<Float>,
    onConfirm: (timeSec: Int, value: Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var timeSec by remember { mutableStateOf(dialog.timeSec) }
    var value by remember { mutableStateOf(dialog.value) }
    val timeUnit = stringResource(CoreRes.string.common_sec)
    val valueUnit = stringResource(dialog.type.unitResource())

    val timeList = remember(dialog.type) {
        (MinTimeSec.toInt()..ProfileEditorDefaults.maxTimeSec(dialog.type)).map { it.toString() }
    }
    val valueList = remember(valueRange) {
        val start = (valueRange.start * 10).roundToInt()
        val end = (valueRange.endInclusive * 10).roundToInt()
        (start..end).map { "${it / 10}.${it % 10}" }
    }

    val contentWidth = if (dialog.type == StepType.Wait) SinglePickerContentWidth else DualPickerContentWidth

    EditorDialog(
        onDismiss = onDismiss,
        modifier = Modifier.width(contentWidth),
        title = {
            GeeFlowDialogTopBar(onCloseClick = onDismiss) {
                StepTypeIcon(dialog.type)
                HorizontalSpacer(12.dp)
                Text(text = dialog.type.label(), style = MaterialTheme.typography.titleMedium)
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(CoreRes.string.common_time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                VerticalSpacer(12.dp)
                GeeFlowInfinitePicker(
                    items = timeList,
                    selected = timeSec.toString(),
                    unit = timeUnit,
                    onSelectionChanged = { timeSec = it.toIntOrNull() ?: timeSec },
                )
            }
            if (dialog.type != StepType.Wait) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = dialog.type.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    VerticalSpacer(12.dp)
                    GeeFlowInfinitePicker(
                        items = valueList,
                        selected = value.formatValue(),
                        unit = valueUnit,
                        onSelectionChanged = { value = it.toFloatOrNull() ?: value },
                    )
                }
            }
        }
        VerticalSpacer(24.dp)
        ConfirmButton { onConfirm(timeSec, value) }
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
private fun StepTypeIcon(type: StepType) = Icon(
    painter = rememberVectorPainter(type.icon()),
    contentDescription = null,
    tint = type.color(),
    modifier = Modifier.size(20.dp),
)

@Composable
private fun ColumnScope.ConfirmButton(enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.align(Alignment.End)) {
        Text(stringResource(CoreRes.string.common_confirm))
    }
}

private fun StepType.unitResource() = when (this) {
    StepType.Pressure -> CoreRes.string.unit_bar
    StepType.Flow, StepType.Wait -> CoreRes.string.unit_milliliters_per_second
}

private const val MinTimeSec = 1f
private const val MinFinishTarget = 1f
private val SinglePickerContentWidth = 208.dp
private val DualPickerContentWidth = DefaultControlWidth * 2 + 16.dp
