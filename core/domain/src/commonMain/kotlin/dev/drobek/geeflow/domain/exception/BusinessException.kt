package dev.drobek.geeflow.domain.exception

sealed class BusinessException(message: String) : Exception(message)

class DeviceNotConnectedException(val deviceId: Long) : BusinessException(
    "Device $deviceId is not connected"
)

class ScaleNotConnectedException(val deviceId: Long) : BusinessException(
    "Smart scale for device $deviceId is not connected"
)
