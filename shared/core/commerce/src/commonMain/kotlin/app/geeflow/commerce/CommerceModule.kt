package app.geeflow.commerce

import org.koin.core.annotation.Module

/**
 * Store build replacement for `NoOpCommerceModule`. Wired into `AppModule` by the
 * `geeflow-store-wiring` skill; the open source build never references it.
 */
@Module(includes = [CommercePlatformModule::class])
class CommerceModule
