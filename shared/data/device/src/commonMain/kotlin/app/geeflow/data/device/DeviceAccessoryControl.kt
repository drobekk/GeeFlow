package app.geeflow.data.device

interface DeviceAccessoryControl {
    suspend fun refreshAccessories()
    suspend fun setSingleDoseGrinderConnectivity(enabled: Boolean)
    suspend fun requestSingleDoseGrinderList()
    suspend fun connectSingleDoseGrinder(name: String)
    suspend fun disconnectSingleDoseGrinder()
    suspend fun runSingleDoseGrinder(grindingSize: Int, grindingSpeed: Int)
    suspend fun setSmartScaleConnectivity(enabled: Boolean)
    suspend fun requestSmartScaleList()
    suspend fun connectSmartScale(name: String)
    suspend fun disconnectSmartScale()
}
