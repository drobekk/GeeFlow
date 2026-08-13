package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Dialog
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_open_settings
import geeflow.shared.core.ui.generated.resources.permission_bluetooth_missing
import geeflow.shared.core.ui.generated.resources.permission_bluetooth_title
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_edit_profile
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceDashboardDialog(
    model: Dialog,
    onEvent: (DeviceDashboardEvent) -> Unit,
) {
    GeeFlowDialog(
        onDismissRequest = { onEvent(DeviceDashboardEvent.DialogDismissed) },
    ) {
        when (model) {
            is Dialog.BluetoothPermissionMissing -> BluetoothPermissionMissingDialog(onEvent)
            is Dialog.BrewDescription -> BrewDescriptionDialog(model, onEvent)
        }
    }
}

@Composable
private fun BrewDescriptionDialog(
    model: Dialog.BrewDescription,
    onEvent: (DeviceDashboardEvent) -> Unit,
) {
    Column(modifier = Modifier.padding(24.dp)) {
        GeeFlowDialogTopBar(
            title = model.name,
            onCloseClick = { onEvent(DeviceDashboardEvent.DialogDismissed) },
        )
        VerticalSpacer(16.dp)
        Text(
            text = model.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
        VerticalSpacer(16.dp)
        Button(
            onClick = { onEvent(DeviceDashboardEvent.EditProfileClicked) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(Res.string.device_dashboard_edit_profile))
        }
    }
}

@Composable
private fun BluetoothPermissionMissingDialog(onEvent: (DeviceDashboardEvent) -> Unit) {
    LifecycleResumeEffect(Unit) {
        onEvent(DeviceDashboardEvent.PermissionDialogResumed)
        onPauseOrDispose {}
    }
    Column(modifier = Modifier.padding(24.dp)) {
        GeeFlowDialogTopBar(
            title = stringResource(CoreRes.string.permission_bluetooth_title),
            onCloseClick = { onEvent(DeviceDashboardEvent.DialogDismissed) },
        )
        VerticalSpacer(16.dp)
        Text(
            text = stringResource(CoreRes.string.permission_bluetooth_missing),
            modifier = Modifier.align(Alignment.Start),
        )
        VerticalSpacer(24.dp)
        Button(
            onClick = { onEvent(DeviceDashboardEvent.OpenSystemSettingsClicked) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(CoreRes.string.common_open_settings))
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    DeviceDashboardDialog(
        model = Dialog.BluetoothPermissionMissing,
        onEvent = {},
    )
}
