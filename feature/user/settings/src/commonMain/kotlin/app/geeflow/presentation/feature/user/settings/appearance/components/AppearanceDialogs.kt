package app.geeflow.presentation.feature.user.settings.appearance.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode
import app.geeflow.platform.getThemeProvider
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.CustomColorChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.CustomColorConfirmed
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DialogDismissed
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.PaletteStyleChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsViewState.Dialog
import app.geeflow.presentation.feature.user.settings.appearance.titleRes
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import geeflow.core.ui.generated.resources.common_confirm
import geeflow.feature.user.settings.generated.resources.Res
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_app_theme
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_custom_color
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_dark_mode
import geeflow.feature.user.settings.generated.resources.user_settings_appearance_palette_style
import org.jetbrains.compose.resources.stringResource
import geeflow.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun AppearanceDialogs(
    dialog: Dialog?,
    currentThemeMode: ThemeMode,
    currentAppTheme: AppTheme,
    currentPaletteStyle: AppPaletteStyle,
    currentCustomSeedColor: Int,
    onEvent: (AppearanceSettingsEvent) -> Unit,
) {
    when (dialog) {
        Dialog.DarkMode -> {
            DarkModeDialog(
                currentThemeMode = currentThemeMode,
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

        Dialog.PaletteStyle -> {
            PaletteStyleDialog(
                currentPaletteStyle = currentPaletteStyle,
                onPaletteStyleSelected = { onEvent(PaletteStyleChanged(it)) },
                onDismiss = { onEvent(DialogDismissed) },
            )
        }

        Dialog.CustomColor -> {
            CustomColorDialog(
                initialColor = Color(currentCustomSeedColor),
                onColorChanged = { onEvent(CustomColorChanged(it.toArgb())) },
                onColorConfirmed = { onEvent(CustomColorConfirmed(it.toArgb())) },
                onDismiss = { onEvent(DialogDismissed) },
            )
        }

        null -> {}
    }
}

@Composable
private fun DarkModeDialog(
    currentThemeMode: ThemeMode,
    onDarkModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) = OptionPickerDialog(
    title = stringResource(Res.string.user_settings_appearance_dark_mode),
    options = ThemeMode.entries,
    selectedOption = currentThemeMode,
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
private fun PaletteStyleDialog(
    currentPaletteStyle: AppPaletteStyle,
    onPaletteStyleSelected: (AppPaletteStyle) -> Unit,
    onDismiss: () -> Unit,
) = OptionPickerDialog(
    title = stringResource(Res.string.user_settings_appearance_palette_style),
    options = AppPaletteStyle.entries,
    selectedOption = currentPaletteStyle,
    optionLabel = { stringResource(it.titleRes) },
    onOptionSelected = onPaletteStyleSelected,
    onDismiss = onDismiss,
)

@Composable
private fun CustomColorDialog(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    onColorConfirmed: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember { mutableStateOf(initialColor) }

    GeeFlowDialog(
        onDismissRequest = {
            onColorChanged(initialColor)
            onDismiss()
        },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GeeFlowDialogTopBar(
                title = stringResource(Res.string.user_settings_appearance_custom_color),
                onCloseClick = {
                    onColorChanged(initialColor)
                    onDismiss()
                },
            )
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(top = 16.dp),
                controller = controller,
                initialColor = initialColor,
                onColorChanged = { envelope ->
                    if (envelope.fromUser) {
                        selectedColor = envelope.color.copy(alpha = 1f)
                        onColorChanged(selectedColor)
                    }
                },
            )
            VerticalSpacer(24.dp)
            Button(
                onClick = { onColorConfirmed(selectedColor) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(CoreRes.string.common_confirm))
            }
            VerticalSpacer(8.dp)
        }
    }
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
