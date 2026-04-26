package app.geeflow.presentation.feature.user.settings.brewing.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.RestoreDefaultProfilesConfirmed
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.TemperatureUnitChanged
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesViewState.Dialog
import app.geeflow.presentation.feature.user.settings.brewing.titleRes
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import geeflow.core.ui.generated.resources.common_confirm
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_restore_default_profiles
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_restore_default_profiles_confirmation
import geeflow.feature.user.settings.generated.resources.user_settings_brewing_temperature_unit
import org.jetbrains.compose.resources.stringResource
import geeflow.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun BrewingPreferencesDialogs(
    dialog: Dialog?,
    currentTemperatureUnit: TemperatureUnit,
    onEvent: (BrewingPreferencesEvent) -> Unit,
) {
    when (dialog) {
        Dialog.TemperatureUnit -> TemperatureUnitDialog(
            currentUnit = currentTemperatureUnit,
            onUnitSelected = { onEvent(TemperatureUnitChanged(it)) },
            onDismiss = { onEvent(DialogDismissed) },
        )

        Dialog.RestoreDefaultProfiles -> RestoreDefaultProfilesDialog(
            onConfirm = { onEvent(RestoreDefaultProfilesConfirmed) },
            onDismiss = { onEvent(DialogDismissed) },
        )

        null -> {}
    }
}

@Composable
private fun TemperatureUnitDialog(
    currentUnit: TemperatureUnit,
    onUnitSelected: (TemperatureUnit) -> Unit,
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
                title = stringResource(resource = Res.string.user_settings_brewing_temperature_unit),
                onCloseClick = onDismiss,
            )
            VerticalSpacer(16.dp)
            TemperatureUnit.entries.forEach { unit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .selectable(
                            selected = unit == currentUnit,
                            onClick = { onUnitSelected(unit) },
                            role = Role.RadioButton,
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = unit == currentUnit, onClick = null)
                    Text(
                        text = stringResource(unit.titleRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
            VerticalSpacer(16.dp)
        }
    }
}

@Composable
private fun RestoreDefaultProfilesDialog(
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
                title = stringResource(resource = Res.string.user_settings_brewing_restore_default_profiles),
                onCloseClick = onDismiss,
            )
            VerticalSpacer(24.dp)
            Text(stringResource(Res.string.user_settings_brewing_restore_default_profiles_confirmation))
            VerticalSpacer(24.dp)
            Button(onClick = onConfirm, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
        }
    }
}
