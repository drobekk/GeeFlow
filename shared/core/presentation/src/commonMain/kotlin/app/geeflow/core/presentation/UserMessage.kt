package app.geeflow.core.presentation

import app.geeflow.domain.exception.AppBackgroundedException
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.exception.MachineBusyException
import app.geeflow.domain.exception.MachineControlChangedException
import app.geeflow.domain.exception.ProfileBindingNotAllowedException
import app.geeflow.domain.exception.ProfileUnavailableException
import app.geeflow.domain.exception.RecordingCapacityExceededException
import app.geeflow.domain.exception.RecordingNoVolumeException
import app.geeflow.domain.exception.RequiredMeasurementLostException
import app.geeflow.domain.exception.ScaleNotConnectedException
import app.geeflow.domain.exception.TelemetryTimeoutException
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.error_app_backgrounded_brew
import geeflow.shared.core.ui.generated.resources.error_connect_device
import geeflow.shared.core.ui.generated.resources.error_generic
import geeflow.shared.core.ui.generated.resources.error_machine_busy
import geeflow.shared.core.ui.generated.resources.error_machine_control_changed
import geeflow.shared.core.ui.generated.resources.error_profile_binding_not_allowed
import geeflow.shared.core.ui.generated.resources.error_profile_unavailable
import geeflow.shared.core.ui.generated.resources.error_recording_no_volume
import geeflow.shared.core.ui.generated.resources.error_recording_too_complex
import geeflow.shared.core.ui.generated.resources.error_required_measurement_lost
import geeflow.shared.core.ui.generated.resources.error_scale_not_connected
import geeflow.shared.core.ui.generated.resources.error_telemetry_timeout
import org.jetbrains.compose.resources.getString

suspend fun Throwable.toUserMessage(): String = when (this) {
    is DeviceNotConnectedException -> getString(Res.string.error_connect_device)
    is ScaleNotConnectedException -> getString(Res.string.error_scale_not_connected)
    is ProfileBindingNotAllowedException -> getString(Res.string.error_profile_binding_not_allowed)
    is MachineBusyException -> getString(Res.string.error_machine_busy)
    is AppBackgroundedException -> getString(Res.string.error_app_backgrounded_brew)
    is RecordingNoVolumeException -> getString(Res.string.error_recording_no_volume)
    is RecordingCapacityExceededException -> getString(Res.string.error_recording_too_complex)
    is MachineControlChangedException -> getString(Res.string.error_machine_control_changed)
    is TelemetryTimeoutException -> getString(Res.string.error_telemetry_timeout)
    is RequiredMeasurementLostException -> getString(Res.string.error_required_measurement_lost)
    is ProfileUnavailableException -> getString(Res.string.error_profile_unavailable)
    else -> getString(Res.string.error_generic)
}
