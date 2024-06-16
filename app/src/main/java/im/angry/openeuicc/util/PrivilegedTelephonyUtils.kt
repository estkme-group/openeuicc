package im.angry.openeuicc.util

import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccSlotMapping
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import kotlinx.coroutines.runBlocking
import java.lang.Exception

val TelephonyManager.supportsDSDS: Boolean
    get() = supportedModemCount == 2

val TelephonyManager.dsdsEnabled: Boolean
    get() = activeModemCount >= 2

fun TelephonyManager.setDsdsEnabled(euiccManager: EuiccChannelManager, enabled: Boolean) {
    val knownChannels = runBlocking {
        euiccManager.enumerateEuiccChannels()
    }

    // Disable all eSIM profiles before performing a DSDS switch (only for internal eSIMs)
    knownChannels.forEach {
        if (!it.removable) {
            it.lpa.disableActiveProfileWithUndo(false)
        }
    }

    switchMultiSimConfig(if (enabled) { 2 } else { 1 })
}

// Disable eSIM profiles before switching the slot mapping
// This ensures that unmapped eSIM ports never have "ghost" profiles enabled
fun TelephonyManager.updateSimSlotMapping(
    euiccManager: EuiccChannelManager, newMapping: Collection<UiccSlotMapping>,
    currentMapping: Collection<UiccSlotMapping> = simSlotMapping
) {
    val unmapped = currentMapping.filterNot { mapping ->
        // If the same physical slot + port pair is not found in the new mapping, it is unmapped
        newMapping.any {
            it.physicalSlotIndex == mapping.physicalSlotIndex && it.portIndex == mapping.portIndex
        }
    }

    val undo = unmapped.mapNotNull { mapping ->
        euiccManager.findEuiccChannelByPortBlocking(mapping.physicalSlotIndex, mapping.portIndex)?.let { channel ->
            if (!channel.removable) {
                return@mapNotNull channel.lpa.disableActiveProfileWithUndo(false)
            } else {
                // Do not do anything for external eUICCs -- we can't really trust them to work properly
                // with no profile enabled.
                return@mapNotNull null
            }
        }
    }

    try {
        simSlotMapping = newMapping
    } catch (e: Exception) {
        e.printStackTrace()
        undo.forEach { it() } // Undo what we just did
        throw e // Rethrow for caller to handle
    }
}

fun SubscriptionManager.tryRefreshCachedEuiccInfo(cardId: Int) {
    if (cardId != 0) {
        try {
            requestEmbeddedSubscriptionInfoListRefresh(cardId)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

// Every EuiccChannel we use here should be backed by a RealUiccPortInfoCompat
val EuiccChannel.removable
    get() = (port as RealUiccPortInfoCompat).card.isRemovable

val EuiccChannel.cardId
    get() = (port as RealUiccPortInfoCompat).card.cardId

val EuiccChannel.isMEP
    get() = (port as RealUiccPortInfoCompat).card.isMultipleEnabledProfilesSupported