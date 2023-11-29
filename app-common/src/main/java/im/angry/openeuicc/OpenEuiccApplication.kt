package im.angry.openeuicc

import android.app.Application
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.EuiccChannelManager

open class OpenEuiccApplication : Application() {
    val telephonyManager by lazy {
        getSystemService(TelephonyManager::class.java)!!
    }

    open val euiccChannelManager: EuiccChannelManager by lazy {
        EuiccChannelManager(this)
    }

    val subscriptionManager by lazy {
        getSystemService(SubscriptionManager::class.java)!!
    }
}