package app.geeflow.domain.exception

sealed class BusinessException(message: String) : Exception(message)

class DeviceNotConnectedException(val deviceId: Long) : BusinessException(
    "Device $deviceId is not connected",
)

class ScaleNotConnectedException(val deviceId: Long) : BusinessException(
    "Smart scale for device $deviceId is not connected",
)

class ProfileBindingNotAllowedException : BusinessException(
    "This profile requires app control and cannot be assigned to the paddle",
)

class MachineBusyException(val deviceId: Long) : BusinessException("The machine is already brewing $deviceId")
class AppBackgroundedException : BusinessException("Keep the app in the foreground to run this profile")
class RecordingNoVolumeException : BusinessException("The recording has no measured volume")
class RecordingCapacityExceededException : BusinessException("The recording exceeds the playback table capacity")
class MachineControlChangedException : BusinessException("Machine control changed")
class TelemetryTimeoutException : BusinessException("Telemetry timed out")
class RequiredMeasurementLostException : BusinessException("Required measurement lost")
class ProfileUnavailableException : BusinessException("Profile unavailable")
