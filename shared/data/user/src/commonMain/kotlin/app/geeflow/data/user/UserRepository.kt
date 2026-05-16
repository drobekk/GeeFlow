package app.geeflow.data.user

import app.geeflow.data.user.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val users: StateFlow<List<User>>
    val selectedUser: Flow<User?>
    fun addUser(user: User): Long
    fun getUserById(id: Long): User?
    fun removeUser(id: Long)
    fun setSelectedUser(id: Long)
    fun renameUser(id: Long, name: String)
    fun updatePhotoUri(id: Long, uri: String?)
    fun setFavoriteDevice(userId: Long, deviceId: Long?)
}
