package dev.drobek.geeflow.presentation.feature.user.list

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.destination.UserList
import dev.drobek.geeflow.presentation.feature.user.list.add.AddUserScreen
import dev.drobek.geeflow.presentation.feature.user.list.add.AddUserViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Single

@Serializable
internal object AddUser : NavKey

@Single
internal class UserListNavFeature : NavFeature {
    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<UserList> {
            val viewModel = koinViewModel<UserListViewModel>()
            UserListScreen(viewModel, navigator)
        }
        entry<AddUser>(metadata = DialogSceneStrategy.dialog(DialogProperties())) {
            val viewModel = koinViewModel<AddUserViewModel>()
            AddUserScreen(viewModel, navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(UserList::class, UserList.serializer())
            subclass(AddUser::class, AddUser.serializer())
        }
    }
}
