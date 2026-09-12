package app.geeflow.presentation.feature.device.dashboard.profileeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowInputPad
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.GeeFlowSlider
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_sec
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

private enum class StepField {
    Time,
    Value,
}

@Composable
private fun StepValuesDialog(
    dialog: ProfileEditorDialog.StepValues,
    valueRange: ClosedFloatingPointRange<Float>,
    onConfirm: (timeSec: Int, value: Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var timeSec by remember { mutableStateOf(dialog.timeSec.toFloat()) }
    var value by remember { mutableStateOf(dialog.value) }
    var padField by remember { mutableStateOf<StepField?>(null) }
    val timeRange = MinTimeSec..ProfileEditorDefaults.maxTimeSec(dialog.type).toFloat()
    val timeUnit = stringResource(CoreRes.string.common_sec)
    val valueUnit = stringResource(dialog.type.unitResource())

    EditorDialog(
        onDismiss = onDismiss,
        title = {
            GeeFlowDialogTopBar(onCloseClick = onDismiss) {
                StepTypeIcon(dialog.type)
                HorizontalSpacer(12.dp)
                Text(text = dialog.type.label(), style = MaterialTheme.typography.titleMedium)
            }
        },
    ) {
        when (padField) {
            StepField.Time -> GeeFlowInputPad(
                valueRange = timeRange,
                unit = timeUnit,
                allowDecimal = false,
                onConfirm = {
                    timeSec = it
                    padField = null
                },
                onBack = { padField = null },
            )

            StepField.Value -> GeeFlowInputPad(
                valueRange = valueRange,
                unit = valueUnit,
                color = dialog.type.color(),
                onConfirm = {
                    value = it
                    padField = null
                },
                onBack = { padField = null },
            )

            null -> {
                DialogSlider(
                    value = timeSec,
                    onValueChange = { timeSec = it },
                    valueRange = timeRange,
                    unit = timeUnit,
                    allowDecimal = false,
                    onClick = { padField = StepField.Time },
                )
                if (dialog.type != StepType.Wait) {
                    VerticalSpacer(12.dp)
                    DialogSlider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = valueRange,
                        unit = valueUnit,
                        color = dialog.type.color(),
                        onClick = { padField = StepField.Value },
                    )
                }
                VerticalSpacer(24.dp)
                ConfirmButton { onConfirm(timeSec.roundToInt(), value) }
            }
        }
    }
}

@Composable
private fun FinishTargetDialog(
    dialog: ProfileEditorDialog.FinishTargetValue,
    onConfirm: (target: Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var target by remember { mutableStateOf(dialog.target) }
    var showPad by remember { mutableStateOf(false) }
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

    EditorDialog(
        onDismiss = onDismiss,
        title = { GeeFlowDialogTopBar(stringResource(title), onDismiss) },
    ) {
        if (showPad) {
            GeeFlowInputPad(
                valueRange = targetRange,
                unit = unit,
                allowDecimal = false,
                onConfirm = {
                    target = it
                    showPad = false
                },
                onBack = { showPad = false },
            )
        } else {
            DialogSlider(
                value = target,
                onValueChange = { target = it },
                valueRange = targetRange,
                unit = unit,
                allowDecimal = false,
                onClick = { showPad = true },
            )
            VerticalSpacer(24.dp)
            ConfirmButton { onConfirm(target) }
        }
    }
}

/** The editor's dialog frame: closing from the top bar is what cancels, so there is no cancel button. */
@Composable
private fun EditorDialog(
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    GeeFlowDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp)
                .fillMaxWidth(),
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

@Composable
private fun DialogSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    allowDecimal: Boolean = true,
) = GeeFlowSlider(
    value = value,
    onValueChange = onValueChange,
    valueRange = valueRange,
    unit = unit,
    color = color,
    allowDecimal = allowDecimal,
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(SliderHeight),
)

private fun StepType.unitResource() = when (this) {
    StepType.Pressure -> CoreRes.string.unit_bar
    StepType.Flow, StepType.Wait -> CoreRes.string.unit_milliliters_per_second
}

private const val MinTimeSec = 1f
private const val MinFinishTarget = 1f
private val SliderHeight = 72.dp
