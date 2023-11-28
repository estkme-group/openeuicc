package im.angry.openeuicc

import android.app.Application
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.BaseEuiccChannelManager

abstract class BaseOpenEuiccApplication : Application() {
    val telephonyManager by lazy {
        getSystemService(TelephonyManager::class.java)!!
    }

    abstract val euiccChannelManager: BaseEuiccChannelManager

    val subscriptionManager by lazy {
        getSystemService(SubscriptionManager::class.java)!!
    }
}