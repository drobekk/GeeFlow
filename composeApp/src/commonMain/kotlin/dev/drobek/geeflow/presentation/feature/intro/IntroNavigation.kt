package dev.drobek.geeflow.presentation.feature.intro

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.presentation.feature.intro.IntroDestinations.CreateUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

interface IntroNavigation : Navigation {
    fun showAddDevice()
}

sealed interface IntroDestinations : NavKey {
    @Serializable
    object CreateUser : IntroDestinations
}

val serializerModuleIntro = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CreateUser::class, CreateUser.serializer())
    }
}

fun EntryProviderScope<NavKey>.introEntries(introNavigation: IntroNavigation) {
    entry<CreateUser> {
        IntroScreen(introNavigation)
    }
}
