package app.geeflow.commerce

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class CommercePlatformModule {

    /** Desktop has no store front, so the About screen keeps hiding the tip jar. */
    @Single
    fun tipFeature(): TipFeature = NoOpTipFeature
}
