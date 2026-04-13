package dev.drobek.geeflow.presentation.feature.user.list

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.destination.UserList
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Single

@Single
internal class UserListNavFeature : NavFeature {
    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<UserList> {
            val viewModel = koinViewModel<UserListViewModel>()
            UserListScreen(viewModel, navigator)
        }
    }

    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(UserList::class, UserList.serializer())
        }
    }
}
