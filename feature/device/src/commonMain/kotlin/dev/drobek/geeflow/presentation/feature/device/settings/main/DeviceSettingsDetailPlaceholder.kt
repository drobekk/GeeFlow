package dev.drobek.geeflow.presentation.feature.device.settings.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.drobek.geeflow.ui.theme.GeeFlowTheme
import geeflow.feature.device.generated.resources.Res
import geeflow.feature.device.generated.resources.device_settings_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSettingsDetailsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().padding(
            horizontal = GeeFlowTheme.spacing.contentHorizontal,
            vertical = GeeFlowTheme.spacing.contentVertical,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(Res.string.device_settings_placeholder))
    }
}
