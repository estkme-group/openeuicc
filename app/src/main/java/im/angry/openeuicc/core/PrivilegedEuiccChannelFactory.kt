package im.angry.openeuicc.core

import android.content.Context
import android.util.Log
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.util.*
import java.lang.IllegalArgumentException

class PrivilegedEuiccChannelFactory(context: Context) : DefaultEuiccChannelFactory(context) {
    private val tm by lazy {
        (context.applicationContext as OpenEuiccApplication).appContainer.telephonyManager
    }

    @Suppress("NAME_SHADOWING")
    override suspend fun tryOpenEuiccChannel(port: UiccPortInfoCompat): EuiccChannel? {
        val port = port as RealUiccPortInfoCompat
        if (port.card.isRemovable) {
            // Attempt unprivileged (OMAPI) before TelephonyManager
            // but still try TelephonyManager in case OMAPI is broken
            super.tryOpenEuiccChannel(port)?.let { return it }
        }

        if (port.card.isEuicc) {
            Log.i(
                DefaultEuiccChannelManager.TAG,
                "Trying TelephonyManager for slot ${port.card.physicalSlotIndex} port ${port.portIndex}"
            )
            try {
                return EuiccChannel(port, TelephonyManagerApduInterface(port, tm))
            } catch (e: IllegalArgumentException) {
                // Failed
                Log.w(
                    DefaultEuiccChannelManager.TAG,
                    "TelephonyManager APDU interface unavailable for slot ${port.card.physicalSlotIndex} port ${port.portIndex}, falling back"
                )
            }
        }

        return super.tryOpenEuiccChannel(port)
    }
}