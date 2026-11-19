package app.geeflow.data.user.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.geeflow.data.user.model.FlowChartRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserSettingsRepositoryImplTest {
    @Test
    fun favoritesPersistIndependentlyAndCanBeRemoved() = runTest {
        val store = InMemoryPreferencesDataStore()
        val repository = UserSettingsRepositoryImpl(store)
        repository.toggleFavoriteProfile(1, "12")
        repository.toggleFavoriteProfile(1, "7")
        val reopened = UserSettingsRepositoryImpl(store)
        assertEquals(setOf("12", "7"), reopened.favoriteProfiles(1).first())
        assertEquals(emptySet(), reopened.favoriteProfiles(2).first())
        reopened.toggleFavoriteProfile(1, "12")
        assertEquals(setOf("7"), reopened.favoriteProfiles(1).first())
        reopened.clearUserSettings(1)
        assertEquals(emptySet(), reopened.favoriteProfiles(1).first())
    }

    @Test
    fun flowChartRangeDefaultsAndPersistsPerUser() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        val repository = UserSettingsRepositoryImpl(dataStore)

        assertEquals(FlowChartRange.FOUR, repository.flowChartRange(userId = 1).first())

        repository.setFlowChartRange(userId = 1, range = FlowChartRange.EIGHT)

        val reopenedRepository = UserSettingsRepositoryImpl(dataStore)
        assertEquals(FlowChartRange.EIGHT, reopenedRepository.flowChartRange(userId = 1).first())
        assertEquals(FlowChartRange.FOUR, reopenedRepository.flowChartRange(userId = 2).first())
    }

    @Test
    fun preferredShotDosePersistsPerUser() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        val repository = UserSettingsRepositoryImpl(dataStore)
        assertEquals(18f, repository.preferredShotDoseGrams(1).first())

        repository.setPreferredShotDoseGrams(1, 15f)

        val reopenedRepository = UserSettingsRepositoryImpl(dataStore)
        assertEquals(15f, reopenedRepository.preferredShotDoseGrams(1).first())
        assertEquals(18f, reopenedRepository.preferredShotDoseGrams(2).first())
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val preferences = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = preferences

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(preferences.value)
        preferences.value = updated
        return updated
    }
}
