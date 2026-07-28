package app.geeflow.data.user.impl

import app.geeflow.data.user.db.AppDatabase
import app.geeflow.data.user.model.User
import org.koin.core.annotation.Singleton

@Singleton
class UsersDao(database: AppDatabase) {
    private val dbQuery = database.userQueries

    fun getAllUsers() = dbQuery
        .selectAll(::mapToUser)
        .executeAsList()

    fun getUserById(id: Long) = dbQuery
        .selectById(id, ::mapToUser)
        .executeAsOneOrNull()

    fun insertUser(user: User): Long {
        var id = 0L
        dbQuery.transaction {
            dbQuery.insertUser(
                id = if (user.id == 0L) null else user.id,
                name = user.name,
                photoUri = user.photoUri,
                isSelected = if (user.isSelected) 1L else 0L,
                favoriteDeviceId = user.favoriteDeviceId,
            )
            id = dbQuery.lastInsertId().executeAsOne()
        }
        return id
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

    fun updateName(id: Long, name: String) {
        dbQuery.updateName(name, id)
    }

    fun updatePhotoUri(id: Long, uri: String?) {
        dbQuery.updatePhotoUri(uri, id)
    }

    private fun mapToUser(
        id: Long,
        name: String,
        photoUri: String?,
        isSelected: Long,
        favoriteDeviceId: Long?,
    ): User = User(
        id = id,
        name = name,
        photoUri = photoUri,
        isSelected = isSelected != 0L,
        favoriteDeviceId = favoriteDeviceId,
    )
}
