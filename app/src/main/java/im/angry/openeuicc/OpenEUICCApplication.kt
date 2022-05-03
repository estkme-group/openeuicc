package im.angry.openeuicc

import android.app.Application
import android.telephony.TelephonyManager
import im.angry.openeuicc.core.EuiccChannelManager

class OpenEUICCApplication : Application() {
    val telephonyManager by lazy {
        getSystemService(TelephonyManager::class.java)!!
    }

    val euiccChannelManager by lazy {
        EuiccChannelManager(this)
    }
}