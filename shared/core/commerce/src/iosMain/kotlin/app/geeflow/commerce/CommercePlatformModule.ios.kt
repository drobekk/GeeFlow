package app.geeflow.commerce

import app.geeflow.commerce.billing.IapTipBilling
import app.geeflow.commerce.ui.StoreTipFeature
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class CommercePlatformModule {

    @Single
    fun tipBilling(): TipBilling = IapTipBilling()

    @Single
    fun tipFeature(tipBilling: TipBilling): TipFeature = StoreTipFeature(billing = tipBilling)
}
