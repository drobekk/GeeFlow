package dev.drobek.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.drobek.geeflow.ui.components.GeeFlowDialog
import dev.drobek.geeflow.ui.components.VerticalSpacer
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.common_open_settings
import geeflow.composeapp.generated.resources.device_dashboard_no_bt_permission
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceDashboardDialog(
    model: DeviceDashboardViewState.Dialog,
    onEvent: (DeviceDashboardEvent) -> Unit,
) {
    GeeFlowDialog(
        onDismissRequest = { onEvent(DeviceDashboardEvent.DialogDismissed) }
    ) {
        when (model) {
            DeviceDashboardViewState.Dialog.BluetoothPermissionMissing -> BluetoothPermissionMissingDialog(onEvent)
        }
    }
}

@Composable
private fun BluetoothPermissionMissingDialog(onEvent: (DeviceDashboardEvent) -> Unit) {
    LifecycleResumeEffect(Unit) {
        onEvent(DeviceDashboardEvent.PermissionDialogResumed)
        onPauseOrDispose {}
    }
    Column(modifier = Modifier.padding(32.dp)) {
        Text(
            text = stringResource(Res.string.device_dashboard_no_bt_permission),
            modifier = Modifier.align(Alignment.Start)
        )
        VerticalSpacer(24.dp)
        Button(
            onClick = { onEvent(DeviceDashboardEvent.OpenSystemSettingsClicked) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(Res.string.common_open_settings))
        }
    }
}

@Composable
@Preview
private fun Preview() = GeeFlowTheme {
    DeviceDashboardDialog(
        model = DeviceDashboardViewState.Dialog.BluetoothPermissionMissing,
        onEvent = {}
    )
}
