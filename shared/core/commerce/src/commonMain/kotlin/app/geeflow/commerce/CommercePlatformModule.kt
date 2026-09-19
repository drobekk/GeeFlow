package app.geeflow.commerce

import org.koin.core.annotation.Module

/**
 * Android and iOS bind the real billing stack; the JVM desktop build falls back to
 * `NoOpTipFeature` because neither Google Play Billing nor StoreKit exists there.
 */
@Module
expect class CommercePlatformModule()
