package im.angry.openeuicc

import android.app.Application
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import java.lang.Exception

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
        // Clean up channels left open in TelephonyManager
        // due to a (potentially) forced restart
        for (slotId in 0 until EuiccChannelManager.MAX_SIMS) {
            for (channel in 0 until 10) {
                try {
                    telephonyManager.iccCloseLogicalChannelBySlot(slotId, channel)
                } catch (_: Exception) {
                    // We do not care
                }
            }
        }
    }
}