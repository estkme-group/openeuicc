package im.angry.openeuicc

import android.app.Application
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.google.android.material.color.DynamicColors
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.core.IEuiccChannelManager
import im.angry.openeuicc.util.PreferenceRepository

open class OpenEuiccApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    val telephonyManager by lazy {
        getSystemService(TelephonyManager::class.java)!!
    }

    open val euiccChannelManager: IEuiccChannelManager by lazy {
        EuiccChannelManager(this)
    }

    val subscriptionManager by lazy {
        getSystemService(SubscriptionManager::class.java)!!
    }

    val preferenceRepository by lazy {
        PreferenceRepository(this)
    }
}