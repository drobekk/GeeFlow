package app.geeflow.data.device

interface DeviceMaintenanceControl {
    suspend fun startCleaning()
    suspend fun stopCleaning()
    suspend fun setCleaningSettings(timeSec: Float, standbySec: Float, count: Int)
}
