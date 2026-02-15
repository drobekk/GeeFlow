package dev.drobek.geeflow.presentation.feature.intro

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.Navigation
import dev.drobek.geeflow.presentation.feature.intro.IntroDestinations.CreateUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder

interface IntroNavigation : Navigation {
    fun showAddDevice()
}

sealed interface IntroDestinations : NavKey {
    @Serializable
    object CreateUser : IntroDestinations
}

fun PolymorphicModuleBuilder<NavKey>.registerIntroSerializers() {
    subclass(CreateUser::class, CreateUser.serializer())
}

fun EntryProviderScope<NavKey>.introEntries(introNavigation: IntroNavigation) {
    entry<CreateUser> {
        IntroScreen(introNavigation)
    }
}
