package app.geeflow.presentation.feature.device.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualEntry
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualError
import geeflow.shared.core.ui.generated.resources.common_open_settings
import geeflow.shared.core.ui.generated.resources.permission_bluetooth_missing
import geeflow.shared.feature.device.add.generated.resources.Res
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_address
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_address_hint
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_change
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_hint
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_invalid_address
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_invalid_serial
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_model
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_model_mismatch
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_no_devices
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_required
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_results
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_scan_failed
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_search
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_search_hint
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_searching
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_select_hint
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_serial
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_serial_match
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as UiRes

@Composable
internal fun ManualDeviceEntry(entry: ManualEntry, onEvent: (AddDeviceEvent) -> Unit) {
    val focus = LocalFocusManager.current
    val search = {
        focus.clearFocus()
        onEvent(AddDeviceEvent.SearchManualDeviceClicked)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text(stringResource(Res.string.add_device_manual_hint), style = MaterialTheme.typography.bodyLarge) }
            item {
                OutlinedTextField(
                    value = entry.serialNumber,
                    onValueChange = { onEvent(AddDeviceEvent.SerialNumberChanged(it)) },
                    label = { Text(stringResource(Res.string.add_device_manual_serial)) },
                    singleLine = true,
                    isError = entry.error == ManualError.SerialRequired || entry.error == ManualError.InvalidSerial,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.add_device_manual_model), style = MaterialTheme.typography.labelLarge)
                        Text(SupportedDevice.byId(entry.modelId)?.model.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedButton(
                        onClick = { onEvent(AddDeviceEvent.ManualModelClicked) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(Res.string.add_device_manual_change)) }
                }
            }
            item {
                OutlinedTextField(
                    value = entry.bluetoothAddress,
                    onValueChange = { onEvent(AddDeviceEvent.BluetoothAddressChanged(it)) },
                    label = { Text(stringResource(Res.string.add_device_manual_address)) },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                    singleLine = true,
                    isError = entry.error == ManualError.InvalidAddress,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    supportingText = { Text(stringResource(Res.string.add_device_manual_address_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { ManualEntryError(entry.error, onEvent) }
            item {
                Button(onClick = search, enabled = !entry.searching, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(Res.string.add_device_manual_search))
                }
            }
            if (entry.searching) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.add_device_manual_searching))
                    }
                }
            }
            if (!entry.searchStarted) {
                item {
                    Text(
                        stringResource(Res.string.add_device_manual_search_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (entry.devices.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.add_device_manual_results), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(Res.string.add_device_manual_select_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(entry.devices, key = { (it.device.connection as DeviceConnection.Ble).peripheralId }) { item ->
                    ManualCandidateCard(item, onEvent)
                }
            } else if (!entry.searching && entry.error == null) {
                item {
                    Text(stringResource(Res.string.add_device_manual_no_devices), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ManualCandidateCard(item: AddDeviceViewState.ManualDeviceItem, onEvent: (AddDeviceEvent) -> Unit) {
    val ble = item.device.connection as DeviceConnection.Ble
    Card(
        onClick = { onEvent(AddDeviceEvent.ManualDeviceClicked(ble.peripheralId)) },
        enabled = item.modelCompatible,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.device.name, style = MaterialTheme.typography.titleMedium)
            Text(ble.macAddress, style = MaterialTheme.typography.bodyLarge)
            if (item.serialMatches) {
                Text(stringResource(Res.string.add_device_manual_serial_match), color = MaterialTheme.colorScheme.primary)
            }
            if (!item.modelCompatible) {
                Text(stringResource(Res.string.add_device_manual_model_mismatch), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ManualEntryError(error: ManualError?, onEvent: (AddDeviceEvent) -> Unit) {
    val message = when (error) {
        ManualError.SerialRequired -> stringResource(Res.string.add_device_manual_required)
        ManualError.InvalidSerial -> stringResource(Res.string.add_device_manual_invalid_serial)
        ManualError.InvalidAddress -> stringResource(Res.string.add_device_manual_invalid_address)
        ManualError.ScanFailed -> stringResource(Res.string.add_device_manual_scan_failed)
        ManualError.PermissionDenied -> stringResource(UiRes.string.permission_bluetooth_missing)
        null -> return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (error == ManualError.PermissionDenied) {
            OutlinedButton(
                onClick = { onEvent(AddDeviceEvent.OpenSystemSettingsClicked) },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(stringResource(UiRes.string.common_open_settings))
            }
        }
    }
}
