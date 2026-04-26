package app.geeflow.presentation.feature.user.settings.appearance.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.DarkMode
import app.geeflow.platform.getThemeProvider
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsViewState.Dialog
import app.geeflow.presentation.feature.user.settings.appearance.titleRes
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_dark_mode
import org.jetbrains.compose.resources.stringResource

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
                onDarkModeSelected = { onEvent(DarkModeChanged(it)) },
                onDismiss = { onEvent(DialogDismissed) },
            )
        }

        Dialog.AppTheme -> {
            AppThemeDialog(
                currentAppTheme = currentAppTheme,
                onAppThemeSelected = { onEvent(AppThemeChanged(it)) },
                onDismiss = { onEvent(DialogDismissed) },
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
    onOptionSelected: (T) -> Unit,
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
                title = title,
                onCloseClick = onDismiss,
            )
            VerticalSpacer(16.dp)
            options.forEach { option ->
                val label = optionLabel(option)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .selectable(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) },
                            role = Role.RadioButton,
                        )
                        .padding(8.dp),
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
            VerticalSpacer(16.dp)
        }
    }
}
