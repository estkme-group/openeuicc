package im.angry.openeuicc.util

import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccSlotMapping
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

private val seId = EuiccChannel.SecureElementId.DEFAULT

val TelephonyManager.supportsDSDS: Boolean
    get() = supportedModemCount == 2

val TelephonyManager.dsdsEnabled: Boolean
    get() = activeModemCount >= 2

fun TelephonyManager.setDsdsEnabled(euiccManager: EuiccChannelManager, enabled: Boolean) {
    // Disable all eSIM profiles before performing a DSDS switch (only for internal eSIMs)
    runBlocking {
        euiccManager.flowInternalEuiccPorts().onEach { (slotId, portId) ->
            euiccManager.withEuiccChannel(slotId, portId, seId) {
                if (!it.port.card.isRemovable) {
                    it.lpa.disableActiveProfile(false)
                }
            }
        }
    }

    switchMultiSimConfig(if (enabled) 2 else 1)
}

// Disable eSIM profiles before switching the slot mapping
// This ensures that unmapped eSIM ports never have "ghost" profiles enabled
suspend fun TelephonyManager.updateSimSlotMapping(
    euiccManager: EuiccChannelManager,
    newMapping: Collection<UiccSlotMapping>,
    currentMapping: Collection<UiccSlotMapping> = simSlotMapping
) {
    val unmapped = currentMapping.filterNot { mapping ->
        // If the same physical slot + port pair is not found in the new mapping, it is unmapped
        newMapping.any {
            it.physicalSlotIndex == mapping.physicalSlotIndex && it.portIndex == mapping.portIndex
        }
    }

    val undo: List<suspend () -> Unit> = unmapped.mapNotNull { mapping ->
        euiccManager.withEuiccChannel(mapping.physicalSlotIndex, mapping.portIndex, seId) { channel ->
            if (!channel.port.card.isRemovable) {
                channel.lpa.disableActiveProfileKeepIccId(false)
            } else {
                // Do not do anything for external eUICCs -- we can't really trust them to work properly
                // with no profile enabled.
                null
            }
        }?.let { iccid ->
            // Generate undo closure because we can't keep reference to `channel` in the closure above
            {
                euiccManager.withEuiccChannel(mapping.physicalSlotIndex, mapping.portIndex, seId) { channel ->
                    channel.lpa.enableProfile(iccid)
                }
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
// except when it is from a USB card reader
val EuiccChannel.cardId
    get() = (port as? RealUiccPortInfoCompat)?.card?.cardId ?: -1

val EuiccChannel.isMEP
    get() = (port as? RealUiccPortInfoCompat)?.card?.isMultipleEnabledProfilesSupported ?: false
