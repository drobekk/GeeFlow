package dev.drobek.geeflow.data.users.api

import dev.drobek.geeflow.domain.user.ChartType
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val visibleCharts: Flow<Set<ChartType>>
    suspend fun setVisibleCharts(types: Set<ChartType>)
}
