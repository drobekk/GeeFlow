package dev.drobek.geeflow.domain.exception

sealed class BusinessException(message: String) : Exception(message)

class DeviceNotConnectedException(val deviceId: String) : BusinessException(
    "Device $deviceId is not connected"
)

class ScaleNotConnectedException(val deviceId: String) : BusinessException(
    "Smart scale for device $deviceId is not connected"
)
