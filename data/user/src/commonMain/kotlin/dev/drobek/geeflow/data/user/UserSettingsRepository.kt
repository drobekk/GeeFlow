package dev.drobek.geeflow.data.user

import dev.drobek.geeflow.data.user.model.ChartType
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val visibleCharts: Flow<Set<ChartType>>
    suspend fun setVisibleCharts(types: Set<ChartType>)
}
