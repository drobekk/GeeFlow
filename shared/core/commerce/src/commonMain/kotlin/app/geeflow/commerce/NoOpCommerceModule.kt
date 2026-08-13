package app.geeflow.commerce

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class NoOpCommerceModule {

    @Single
    fun tipFeature(): TipFeature = NoOpTipFeature
}
