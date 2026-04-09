package dev.drobek.geeflow.data.user.impl

import dev.drobek.geeflow.data.db.DatabaseProvider
import dev.drobek.geeflow.data.user.model.User
import org.koin.core.annotation.Singleton

@Singleton
class UsersDao(databaseProvider: DatabaseProvider) {
    private val dbQuery = databaseProvider.database.userQueries

    fun getAllUsers() = dbQuery
        .selectAll(::mapToUser)
        .executeAsList()

    fun getUserById(id: Long) = dbQuery
        .selectById(id, ::mapToUser)
        .executeAsOneOrNull()

    fun insertUser(user: User) {
        dbQuery.insertUser(
            id = if (user.id == 0L) null else user.id,
            name = user.name,
            photoUri = user.photoUri,
            isSelected = if (user.isSelected) 1L else 0L,
            favoriteDeviceId = user.favoriteDeviceId
        )
    }

    fun deleteUser(id: Long) {
        dbQuery.deleteById(id)
    }

    fun setSelectedUser(id: Long) {
        dbQuery.transaction {
            dbQuery.resetSelected()
            dbQuery.setSelected(id)
        }
    }

    fun setFavoriteDevice(userId: Long, deviceId: Long?) {
        dbQuery.setFavoriteDevice(deviceId, userId)
    }

    private fun mapToUser(
        id: Long,
        name: String,
        photoUri: String?,
        isSelected: Long,
        favoriteDeviceId: Long?
    ): User = User(
        id = id,
        name = name,
        photoUri = photoUri,
        isSelected = isSelected != 0L,
        favoriteDeviceId = favoriteDeviceId
    )
}
