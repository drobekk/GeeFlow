package app.geeflow.presentation.feature.device.settings.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_settings_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSettingsDetailsPlaceholder() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(
                horizontal = GeeFlowTheme.spacing.contentHorizontal,
                vertical = GeeFlowTheme.spacing.contentVertical,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(Res.string.device_settings_placeholder))
    }
}
