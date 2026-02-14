package dev.drobek.geeflow.data.users.api

import dev.drobek.geeflow.domain.user.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val users: StateFlow<List<User>>
    fun addUser(user: User)
    fun getUserById(id: Long): User?
    fun removeUser(id: Long)
}
