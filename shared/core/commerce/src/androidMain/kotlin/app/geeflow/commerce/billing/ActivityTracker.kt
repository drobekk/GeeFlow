package app.geeflow.commerce.billing

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.multiplatform.inAppPurchase.IAPManager

/**
 * Google Play Billing needs an Activity to show the purchase sheet. Tracking it through the
 * application lifecycle keeps the launcher module free of any commerce code.
 */
class ActivityTracker(private val application: Application) {

    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(Callbacks())
    }

    fun bind(iapManager: IAPManager) {
        iapManager.setContext(context = application, activity = currentActivity)
    }

    private inner class Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) {
            currentActivity = activity
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
        }

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) {
            if (currentActivity === activity) currentActivity = null
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity === activity) currentActivity = null
        }
    }
}
