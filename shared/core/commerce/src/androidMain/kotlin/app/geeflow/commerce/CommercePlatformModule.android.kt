package app.geeflow.commerce

import android.app.Application
import android.content.Context
import app.geeflow.commerce.billing.ActivityTracker
import app.geeflow.commerce.billing.IapTipBilling
import app.geeflow.commerce.ui.StoreTipFeature
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class CommercePlatformModule {

    @Single
    fun activityTracker(context: Context): ActivityTracker =
        ActivityTracker(context.applicationContext as Application)

    @Single
    fun tipBilling(activityTracker: ActivityTracker): TipBilling =
        IapTipBilling(prepare = activityTracker::bind)

    @Single
    fun tipFeature(tipBilling: TipBilling): TipFeature = StoreTipFeature(billing = tipBilling)
}
