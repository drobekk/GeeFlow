package dev.drobek.geeflow.data.users.impl

import dev.drobek.geeflow.data.db.DatabaseProvider
import dev.drobek.geeflow.domain.user.model.User
import org.koin.core.annotation.Singleton

@Singleton
class UsersDao(databaseProvider: DatabaseProvider) {
    private val dbQuery = databaseProvider.database.userQueries

    fun getAllUsers() = dbQuery.selectAll(::mapToUser).executeAsList()

    fun getUserById(id: Long) = dbQuery.selectById(id, ::mapToUser).executeAsOneOrNull()

    fun insertUser(user: User) {
        dbQuery.insertUser(
            id = if (user.id == 0L) null else user.id,
            name = user.name,
            photoUri = user.photoUri
        )
    }

    fun deleteUser(id: Long) {
        dbQuery.deleteById(id)
    }

    private fun mapToUser(
        id: Long,
        name: String,
        photoUri: String?
    ): User = User(id, name, photoUri)
}
