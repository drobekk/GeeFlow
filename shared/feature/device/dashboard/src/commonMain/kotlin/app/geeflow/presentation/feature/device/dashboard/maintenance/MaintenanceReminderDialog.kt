package app.geeflow.presentation.feature.device.dashboard.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.data.device.model.CleaningType
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.maintenance_daily
import geeflow.shared.core.ui.generated.resources.maintenance_daily_description
import geeflow.shared.core.ui.generated.resources.maintenance_deep
import geeflow.shared.core.ui.generated.resources.maintenance_deep_description
import geeflow.shared.core.ui.generated.resources.maintenance_reminder_both
import geeflow.shared.core.ui.generated.resources.maintenance_reminder_due
import geeflow.shared.core.ui.generated.resources.maintenance_reminder_open
import geeflow.shared.core.ui.generated.resources.maintenance_reminder_skip
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MaintenanceReminderHost(
    deviceId: Long,
    blocked: Boolean,
    onOpen: (CleaningType) -> Unit,
    onError: () -> Unit,
) {
    val viewModel = koinViewModel<MaintenanceReminderViewModel>(key = "maintenance-$deviceId") {
        parametersOf(deviceId)
    }
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var active by remember { mutableStateOf(false) }
    val visible = state.eligible && state.dueTypes.isNotEmpty()

    LifecycleResumeEffect(viewModel) {
        active = true
        viewModel.resumed()
        onPauseOrDispose { active = false }
    }
    EventsDispatcher(viewModel.events) { onError() }

    if (active && !blocked && visible) {
        MaintenanceReminderDialog(
            state = state,
            onOpen = onOpen,
            onSkip = { viewModel.skip(state.dueTypes) },
        )
    }
}

@Composable
private fun MaintenanceReminderDialog(
    state: MaintenanceReminderViewState,
    onOpen: (CleaningType) -> Unit,
    onSkip: () -> Unit,
) {
    val type = if (CleaningType.Deep in state.dueTypes) CleaningType.Deep else CleaningType.Daily
    val title = stringResource(
        if (type == CleaningType.Daily) Res.string.maintenance_daily else Res.string.maintenance_deep,
    )
    GeeFlowDialog(
        onDismissRequest = { if (!state.busy) onSkip() },
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.maintenance_reminder_due, title),
                onCloseClick = { if (!state.busy) onSkip() },
            )
            VerticalSpacer(16.dp)
            Text(
                text = stringResource(
                    if (type == CleaningType.Daily) {
                        Res.string.maintenance_daily_description
                    } else {
                        Res.string.maintenance_deep_description
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.dueTypes.size > 1) {
                Text(
                    text = stringResource(Res.string.maintenance_reminder_both),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            VerticalSpacer(24.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            ) {
                TextButton(onClick = onSkip, enabled = !state.busy) {
                    Text(stringResource(Res.string.maintenance_reminder_skip))
                }
                Button(onClick = { onOpen(type) }, enabled = !state.busy) {
                    Text(stringResource(Res.string.maintenance_reminder_open))
                }
            }
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    MaintenanceReminderDialog(
        state = MaintenanceReminderViewState(dueTypes = CleaningType.entries.toSet()),
        onOpen = {},
        onSkip = {},
    )
}
