package im.angry.openeuicc.core

import android.content.Context
import android.util.Log
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.util.*
import java.lang.Exception
import java.lang.IllegalArgumentException

class PrivilegedEuiccChannelManager(context: Context): EuiccChannelManager(context) {
    override val uiccCards: Collection<UiccCardInfoCompat>
        get() = tm.uiccCardsInfoCompat

    @Suppress("NAME_SHADOWING")
    override fun tryOpenEuiccChannelPrivileged(port: UiccPortInfoCompat): EuiccChannel? {
        val port = port as RealUiccPortInfoCompat
        if (port.card.isRemovable) {
            // Attempt unprivileged (OMAPI) before TelephonyManager
            // but still try TelephonyManager in case OMAPI is broken
            super.tryOpenEuiccChannelUnprivileged(port)?.let { return it }
        }

        if (port.card.isEuicc) {
            Log.i(TAG, "Trying TelephonyManager for slot ${port.card.physicalSlotIndex} port ${port.portIndex}")
            try {
                return TelephonyManagerChannel(port, tm)
            } catch (e: IllegalArgumentException) {
                // Failed
                Log.w(TAG, "TelephonyManager APDU interface unavailable for slot ${port.card.physicalSlotIndex} port ${port.portIndex}, falling back")
            }
        }
        return null
    }

    // Clean up channels left open in TelephonyManager
    // due to a (potentially) forced restart
    // This should be called every time the application is restarted
    fun closeAllStaleChannels() {
        for (card in tm.uiccCardsInfo) {
            for (channel in 0 until 10) {
                try {
                    tm.iccCloseLogicalChannelBySlot(card.slotIndex, channel)
                } catch (_: Exception) {
                    // We do not care
                }
            }
        }
    }

    override fun notifyEuiccProfilesChanged(logicalSlotId: Int) {
        (context.applicationContext as OpenEuiccApplication).subscriptionManager.apply {
            findEuiccChannelBySlotBlocking(logicalSlotId)?.let {
                tryRefreshCachedEuiccInfo(it.cardId)
            }
        }
    }
}