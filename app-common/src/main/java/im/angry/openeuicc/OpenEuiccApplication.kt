package im.angry.openeuicc

import android.app.Application
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.EuiccChannelManager

class OpenEuiccApplication : Application() {
    val telephonyManager by lazy {
        getSystemService(TelephonyManager::class.java)!!
    }

    val euiccChannelManager by lazy {
        EuiccChannelManager(this)
    }

    val subscriptionManager by lazy {
        getSystemService(SubscriptionManager::class.java)!!
    }

    override fun onCreate() {
        super.onCreate()
        euiccChannelManager.closeAllStaleChannels()
    }
}