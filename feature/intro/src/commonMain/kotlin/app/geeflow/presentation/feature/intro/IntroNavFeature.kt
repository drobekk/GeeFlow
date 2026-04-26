package app.geeflow.presentation.feature.intro

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.geeflow.navigation.NavFeature
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.destination.Intro
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
