package dev.drobek.geeflow.data.user.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.drobek.geeflow.data.user.UserSettingsRepository
import dev.drobek.geeflow.data.user.model.ChartType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class UserSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : UserSettingsRepository {

    private val visibleChartsKey = stringSetPreferencesKey("visible_charts")

    override val visibleCharts: Flow<Set<ChartType>> = dataStore.data.map { preferences ->
        val stringSet = preferences[visibleChartsKey]
        stringSet
            ?.mapNotNull {
                try {
                    ChartType.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }?.toSet()
            ?: setOf(ChartType.PRESSURE, ChartType.FLOW_RATE, ChartType.WEIGHT_RATE)
    }

    override suspend fun setVisibleCharts(types: Set<ChartType>) {
        dataStore.edit { preferences ->
            preferences[visibleChartsKey] = types.map { it.name }.toSet()
        }
    }
}
