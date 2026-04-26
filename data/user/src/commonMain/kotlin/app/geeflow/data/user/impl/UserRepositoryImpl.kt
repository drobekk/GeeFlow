package app.geeflow.data.user.impl

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Singleton

@Singleton
class UserRepositoryImpl(
    private val usersDao: UsersDao,
) : UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()
    override val selectedUser: Flow<User?> = users.map { users -> users.find { it.isSelected } }

    init {
        refresh()
    }

    override fun addUser(user: User): Long {
        val id = usersDao.insertUser(user)
        refresh()
        return id
    }

    override fun getUserById(id: Long): User? = usersDao.getUserById(id)

    override fun removeUser(id: Long) {
        val wasSelected = usersDao.getUserById(id)?.isSelected == true
        usersDao.deleteUser(id)
        if (wasSelected) {
            usersDao.getAllUsers().firstOrNull()?.let { usersDao.setSelectedUser(it.id) }
        }
        refresh()
    }

    override fun setSelectedUser(id: Long) {
        usersDao.setSelectedUser(id)
        refresh()
    }

    override fun renameUser(id: Long, name: String) {
        usersDao.updateName(id, name)
        refresh()
    }

    override fun setFavoriteDevice(userId: Long, deviceId: Long?) {
        usersDao.setFavoriteDevice(userId, deviceId)
        refresh()
    }

    private fun refresh() {
        _users.value = usersDao.getAllUsers()
    }
}
