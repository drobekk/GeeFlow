package dev.drobek.geeflow.presentation.feature.intro

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.drobek.geeflow.navigation.NavFeature
import dev.drobek.geeflow.navigation.Navigator
import dev.drobek.geeflow.navigation.destination.Intro
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.annotation.Single

@Single
internal class IntroNavFeature : NavFeature {
    override val serializerModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Intro::class, Intro.serializer())
        }
    }

    override fun EntryProviderScope<NavKey>.provideEntries(navigator: Navigator) {
        entry<Intro> {
            IntroScreen(navigator)
        }
    }
}
