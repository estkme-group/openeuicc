package im.angry.openeuicc.util

import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.UiccSlotMapping
import im.angry.openeuicc.core.EuiccChannelManager
import net.typeblog.lpac_jni.LocalProfileInfo
import java.lang.Exception

val TelephonyManager.supportsDSDS: Boolean
    get() = supportedModemCount == 2

var TelephonyManager.dsdsEnabled: Boolean
    get() = activeModemCount >= 2
    set(value) {
        switchMultiSimConfig(if (value) { 2 } else {1})
    }

// Disable eSIM profiles before switching the slot mapping
// This ensures that unmapped eSIM ports never have "ghost" profiles enabled
fun TelephonyManager.updateSimSlotMapping(euiccManager: EuiccChannelManager, newMapping: Collection<UiccSlotMapping>) {
    val unmapped = simSlotMapping.filterNot { mapping ->
        // If the same physical slot + port pair is not found in the new mapping, it is unmapped
        newMapping.any {
            it.physicalSlotIndex == mapping.physicalSlotIndex && it.portIndex == mapping.portIndex
        }
    }

    val undo = unmapped.mapNotNull { mapping ->
        euiccManager.findEuiccChannelByPortBlocking(mapping.physicalSlotIndex, mapping.portIndex)?.let { channel ->
            channel.lpa.profiles.find { it.state == LocalProfileInfo.State.Enabled }?.let { profile ->
                channel.lpa.disableProfile(profile.iccid)
                return@mapNotNull { channel.lpa.enableProfile(profile.iccid) }
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