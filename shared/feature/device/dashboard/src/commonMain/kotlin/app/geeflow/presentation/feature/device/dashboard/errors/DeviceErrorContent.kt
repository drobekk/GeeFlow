package app.geeflow.presentation.feature.device.dashboard.errors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.data.device.model.DeviceError
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_brew_sensor
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_code
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_extraction_timeout
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_heating_timeout
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_none
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_pressure_sensor
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_steam_sensor
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_title
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_unknown
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_water_level
import geeflow.shared.feature.device.dashboard.generated.resources.device_error_water_replenishment_timeout
import geeflow.shared.feature.device.dashboard.generated.resources.device_water_alarm_error
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceErrorContent(error: DeviceError?, onClose: () -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        GeeFlowDialogTopBar(title = stringResource(Res.string.device_error_title), onCloseClick = onClose)
        VerticalSpacer(16.dp)
        val description = when (error?.type) {
            DeviceError.Type.WaterShortage -> stringResource(Res.string.device_water_alarm_error)
            DeviceError.Type.HeatingTimeout -> stringResource(Res.string.device_error_heating_timeout)
            DeviceError.Type.WaterReplenishmentTimeout -> stringResource(Res.string.device_error_water_replenishment_timeout)
            DeviceError.Type.ExtractionTimeout -> stringResource(Res.string.device_error_extraction_timeout)
            DeviceError.Type.PressureSensorMissing -> stringResource(Res.string.device_error_pressure_sensor)
            DeviceError.Type.SteamBoilerSensorFailure -> stringResource(Res.string.device_error_steam_sensor)
            DeviceError.Type.BrewBoilerSensorFailure -> stringResource(Res.string.device_error_brew_sensor)
            DeviceError.Type.WaterLevelAbnormal -> stringResource(Res.string.device_error_water_level)
            null -> if (error == null) {
                stringResource(Res.string.device_error_none)
            } else {
                stringResource(Res.string.device_error_unknown, error.code)
            }
        }
        Text(text = description, style = MaterialTheme.typography.bodyLarge)
        if (error != null) {
            VerticalSpacer(12.dp)
            Text(
                text = stringResource(Res.string.device_error_code, error.code),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
