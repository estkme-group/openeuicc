package im.angry.openeuicc.util

import android.os.Build
import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.TelephonyManager

// TODO: Usage of *byPort APIs will still break build in-tree on lower AOSP versions
//       Maybe older versions should simply include hidden-apis-shim when building?
fun TelephonyManager.iccOpenLogicalChannelByPortCompat(
    slotIndex: Int, portIndex: Int, aid: String?, p2: Int
): IccOpenLogicalChannelResponse =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        iccOpenLogicalChannelByPort(slotIndex, portIndex, aid, p2)
    } else {
        iccOpenLogicalChannelBySlot(slotIndex, aid, p2)
    }

fun TelephonyManager.iccCloseLogicalChannelByPortCompat(
    slotIndex: Int, portIndex: Int, channel: Int
) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        iccCloseLogicalChannelByPort(slotIndex, portIndex, channel)
    } else {
        iccCloseLogicalChannelBySlot(slotIndex, channel)
    }

fun TelephonyManager.iccTransmitApduLogicalChannelByPortCompat(
    slotIndex: Int, portIndex: Int, channel: Int,
    cla: Int, inst: Int, p1: Int, p2: Int, p3: Int, data: String?
): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        iccTransmitApduLogicalChannelByPort(
            slotIndex, portIndex, channel, cla, inst, p1, p2, p3, data
        )
    } else {
        iccTransmitApduLogicalChannelBySlot(
            slotIndex, channel, cla, inst, p1, p2, p3, data
        )
    }