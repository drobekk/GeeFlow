package dev.drobek.geeflow.data.user

import dev.drobek.geeflow.data.user.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val users: StateFlow<List<User>>
    val selectedUser: Flow<User?>
    fun addUser(user: User)
    fun getUserById(id: Long): User?
    fun removeUser(id: Long)
    fun setSelectedUser(id: Long)
    fun setFavoriteDevice(userId: Long, deviceId: Long?)
}
