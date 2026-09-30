package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewHistoryRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.model.DeviceBrewingSettings
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveBrewToHistoryUseCaseTest {
    @Test
    fun `manual history follows device settings for every selected user`() = runTest {
        val users = TestUsers()
        val history = TestHistory()
        val settings = object : DeviceBrewingSettingsRepository {
            override fun observe(deviceId: Long) = flowOf(DeviceBrewingSettings(treatManualAsFlush = deviceId == 1L))
            override suspend fun save(deviceId: Long, settings: DeviceBrewingSettings) = Unit
            override suspend fun remove(deviceId: Long) = Unit
        }
        val save = SaveBrewToHistoryUseCase(history, settings, users)
        for (userId in listOf(1L, 2L)) {
            users.setSelectedUser(userId)
            save(1L, session(), null, null)
            assertEquals((userId - 1).toInt(), history.entries.size)
            save(2L, session(), null, null)
            assertEquals(userId, history.entries.last().userId)
        }
        assertEquals(listOf(1L, 2L), history.entries.map { it.userId })
        save(1L, session().copy(mode = BrewMode.Profile), null, null)
        save(1L, session().copy(mode = BrewMode.Freehand), null, null)
        assertEquals(4, history.entries.size)
    }

    private fun session() = BrewSession(
        userId = 1,
        startTime = Instant.fromEpochSeconds(100),
        elapsedSeconds = 5,
        dataPoints = mapOf(1f to BrewDataPoint(9f, 0f, 10f, 2f, 0f)),
    )

    private class TestHistory : BrewHistoryRepository {
        val entries = mutableListOf<BrewHistoryEntry>()
        override fun addBrew(entry: BrewHistoryEntry, dataPoints: Map<Float, BrewDataPoint>) {
            entries.add(entry)
        }
        override fun getBrews(userId: Long, query: String, limit: Int, offset: Int) = entries.toList()
        override fun getBrewDataPoints(id: Long) = emptyMap<Float, BrewDataPoint>()
        override fun deleteBrew(id: Long) = Unit
    }

    private class TestUsers : UserRepository {
        override val users = MutableStateFlow(listOf(User(id = 1, name = "First"), User(id = 2, name = "Second")))
        override val selectedUser = MutableStateFlow<User?>(users.value.first())
        override fun setSelectedUser(id: Long) {
            selectedUser.value = users.value.first { it.id == id }
        }
        override fun addUser(user: User) = user.id
        override fun getUserById(id: Long) = users.value.find { it.id == id }
        override fun removeUser(id: Long) = Unit
        override fun renameUser(id: Long, name: String) = Unit
        override fun updatePhotoUri(id: Long, uri: String?) = Unit
        override fun setFavoriteDevice(userId: Long, deviceId: Long?) = Unit
    }
}
