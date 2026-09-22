package app.geeflow.data.device

import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.MaintenanceSettings
import kotlinx.coroutines.flow.Flow

interface MaintenanceSettingsRepository {
    fun observe(deviceId: Long): Flow<MaintenanceSettings?>
    suspend fun initialize(deviceId: Long, settings: MaintenanceSettings)
    suspend fun save(deviceId: Long, settings: MaintenanceSettings, today: Long)
    suspend fun postpone(deviceId: Long, types: Set<CleaningType>, today: Long)
    suspend fun remove(deviceId: Long)
}
