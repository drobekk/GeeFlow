package app.geeflow.data.device

import app.geeflow.data.device.model.AutoFlushSettings
import kotlinx.coroutines.flow.Flow

interface AutoFlushSettingsRepository {
    fun observe(deviceId: Long): Flow<AutoFlushSettings>
    suspend fun save(deviceId: Long, settings: AutoFlushSettings)
    suspend fun remove(deviceId: Long)
}
