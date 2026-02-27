package dev.drobek.geeflow.data.users.impl

import dev.drobek.geeflow.data.db.DatabaseProvider
import dev.drobek.geeflow.domain.user.model.User
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
            isSelected = user.isSelected,
            favoriteDeviceMacAddress = user.favoriteDeviceMacAddress
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

    fun setFavoriteDevice(userId: Long, deviceMacAddress: String?) {
        dbQuery.setFavoriteDevice(deviceMacAddress, userId)
    }

    private fun mapToUser(
        id: Long,
        name: String,
        photoUri: String?,
        isSelected: Boolean,
        favoriteDeviceMacAddress: String?
    ): User = User(
        id = id,
        name = name,
        photoUri = photoUri,
        isSelected = isSelected,
        favoriteDeviceMacAddress = favoriteDeviceMacAddress
    )
}
