package im.angry.openeuicc.core

import android.content.Context
import android.telephony.UiccCardInfo
import android.util.Log
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.util.*
import java.lang.Exception
import java.lang.IllegalArgumentException

class PrivilegedEuiccChannelManager(context: Context): EuiccChannelManager(context) {
    override fun checkPrivileges() = true // TODO: Implement proper system app check

    override fun tryOpenEuiccChannelPrivileged(uiccInfo: UiccCardInfo, channelInfo: EuiccChannelInfo): EuiccChannel? {
        if (uiccInfo.isRemovable) {
            // Attempt unprivileged (OMAPI) before TelephonyManager
            // but still try TelephonyManager in case OMAPI is broken
            super.tryOpenEuiccChannelUnprivileged(uiccInfo, channelInfo)?.let { return it }
        }

        if (uiccInfo.isEuicc) {
            Log.i(TAG, "Trying TelephonyManager for slot ${uiccInfo.slotIndex}")
            // TODO: On Tiramisu, we should also connect all available "ports" for MEP support
            try {
                return TelephonyManagerChannel(channelInfo, tm)
            } catch (e: IllegalArgumentException) {
                // Failed
                Log.w(TAG, "TelephonyManager APDU interface unavailable for slot ${uiccInfo.slotIndex}, falling back")
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

    override fun notifyEuiccProfilesChanged(slotId: Int) {
        (context.applicationContext as OpenEuiccApplication).subscriptionManager.apply {
            findEuiccChannelBySlotBlocking(slotId)?.let {
                tryRefreshCachedEuiccInfo(it.cardId)
            }
        }
    }
}