package dev.drobek.geeflow.data.users.impl

import dev.drobek.geeflow.data.users.api.UserRepository
import dev.drobek.geeflow.domain.user.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Singleton

@Singleton
class UserRepositoryImpl(
    private val usersDao: UsersDao
) : UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        refresh()
    }

    override fun addUser(user: User) {
        usersDao.insertUser(user)
        refresh()
    }

    override fun getUserById(id: Long): User? {
        return usersDao.getUserById(id)
    }

    override fun removeUser(id: Long) {
        usersDao.deleteUser(id)
        refresh()
    }

    private fun refresh() {
        _users.value = usersDao.getAllUsers()
    }
}
