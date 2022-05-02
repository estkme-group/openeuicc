package im.angry.openeuicc.util

import android.telephony.TelephonyManager

val TelephonyManager.supportsDSDS: Boolean
    get() = supportedModemCount == 2

var TelephonyManager.dsdsEnabled: Boolean
    get() = activeModemCount >= 2
    set(value) {
        switchMultiSimConfig(if (value) { 2 } else {1})
    }