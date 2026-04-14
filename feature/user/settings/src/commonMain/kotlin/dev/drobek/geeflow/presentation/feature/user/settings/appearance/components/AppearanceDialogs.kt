package dev.drobek.geeflow.presentation.feature.user.settings.appearance.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.drobek.geeflow.data.user.model.AppTheme
import dev.drobek.geeflow.data.user.model.DarkMode
import dev.drobek.geeflow.platform.getThemeProvider
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsViewState.Dialog
import dev.drobek.geeflow.presentation.feature.user.settings.appearance.titleRes
import geeflow.core.ui.generated.resources.common_cancel
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_dark_mode
import org.jetbrains.compose.resources.stringResource
import geeflow.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun AppearanceDialogs(
    dialog: Dialog?,
    currentDarkMode: DarkMode,
    currentAppTheme: AppTheme,
    onEvent: (AppearanceSettingsEvent) -> Unit,
) {
    when (dialog) {
        Dialog.DarkMode -> {
            DarkModeDialog(
                currentDarkMode = currentDarkMode,
                onDarkModeSelected = { onEvent(AppearanceSettingsEvent.DarkModeChanged(it)) },
                onDismiss = { onEvent(AppearanceSettingsEvent.DialogDismissed) },
            )
        }

        Dialog.AppTheme -> {
            AppThemeDialog(
                currentAppTheme = currentAppTheme,
                onAppThemeSelected = { onEvent(AppearanceSettingsEvent.AppThemeChanged(it)) },
                onDismiss = { onEvent(AppearanceSettingsEvent.DialogDismissed) },
            )
        }

        null -> {}
    }
}

@Composable
private fun DarkModeDialog(
    currentDarkMode: DarkMode,
    onDarkModeSelected: (DarkMode) -> Unit,
    onDismiss: () -> Unit,
) = OptionPickerDialog(
    title = stringResource(Res.string.user_settings_appearance_dark_mode),
    options = DarkMode.entries,
    selectedOption = currentDarkMode,
    optionLabel = { stringResource(it.titleRes) },
    cancelLabel = stringResource(CoreRes.string.common_cancel),
    onOptionSelected = onDarkModeSelected,
    onDismiss = onDismiss,
)

@Composable
internal fun AppThemeDialog(
    currentAppTheme: AppTheme,
    onAppThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val isSystemThemeAvailable = getThemeProvider().getSystemColorScheme(isSystemInDarkTheme()) != null

    OptionPickerDialog(
        title = stringResource(Res.string.user_settings_appearance_app_theme),
        options = AppTheme.entries.filter {
            it != AppTheme.SYSTEM || isSystemThemeAvailable
        },
        selectedOption = currentAppTheme,
        optionLabel = { stringResource(it.titleRes) },
        cancelLabel = stringResource(CoreRes.string.common_cancel),
        onOptionSelected = onAppThemeSelected,
        onDismiss = onDismiss,
    )
}

@Composable
private fun <T> OptionPickerDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    cancelLabel: String,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    val label = optionLabel(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selectedOption,
                                onClick = { onOptionSelected(option) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = null,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
    )
}
