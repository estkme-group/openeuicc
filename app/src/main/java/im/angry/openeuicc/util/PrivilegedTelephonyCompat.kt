package im.angry.openeuicc.util

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.TelephonyManager
import android.telephony.UiccCardInfo
import android.telephony.UiccPortInfo
import java.lang.RuntimeException

/*
 * Implementation of Uicc{Card,Port}InfoCompat when privileged.
 * Also handles compatibility with different platform API versions.
 */
@Suppress("DEPRECATION")
class RealUiccCardInfoCompat(val inner: UiccCardInfo): UiccCardInfoCompat {
    override val physicalSlotIndex: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.physicalSlotIndex
            } else {
                inner.slotIndex
            }

    override val ports: Collection<RealUiccPortInfoCompat>
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.ports.map { RealUiccPortInfoCompat(it, this) }
            } else {
                listOf(RealUiccPortInfoCompat(null, this))
            }

    val isEuicc: Boolean
        get() = inner.isEuicc

    override val isRemovable: Boolean
        get() = inner.isRemovable

    val isMultipleEnabledProfilesSupported: Boolean
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.isMultipleEnabledProfilesSupported
            } else {
                false
            }

    val cardId: Int
        get() = inner.cardId
}

class RealUiccPortInfoCompat(
    private val _inner: Any?,
    override val card: RealUiccCardInfoCompat
): UiccPortInfoCompat {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            check(_inner != null && _inner is UiccPortInfo) {
                "_inner is not UiccPortInfo on TIRAMISU"
            }
        }
    }

    private val inner: UiccPortInfo
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                _inner as UiccPortInfo
            } else {
                throw RuntimeException("UiccPortInfo does not exist before T")
            }

    override val portIndex: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.portIndex
            } else {
                0
            }

    override val logicalSlotIndex: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.logicalSlotIndex
            } else {
                card.physicalSlotIndex // logical is the same as physical below TIRAMISU
            }
}

val TelephonyManager.uiccCardsInfoCompat: List<RealUiccCardInfoCompat>
    @SuppressLint("MissingPermission")
    get() = uiccCardsInfo.map { RealUiccCardInfoCompat(it) }

// TODO: Usage of new APIs from T or later will still break build in-tree on lower AOSP versions
//       Maybe older versions should simply include hidden-apis-shim when building?
fun TelephonyManager.iccOpenLogicalChannelByPortCompat(
    slotIndex: Int, portIndex: Int, aid: String?, p2: Int
): IccOpenLogicalChannelResponse =
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        iccOpenLogicalChannelByPort(slotIndex, portIndex, aid, p2)!!
    } else {
        iccOpenLogicalChannelBySlot(slotIndex, aid, p2)!!
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
    slotIndex: Int,
    portIndex: Int,
    channel: Int,
    tx: ByteArray
): String? {
    val cla = tx[0].toUByte().toInt()
    val ins = tx[1].toUByte().toInt()
    val p1 = tx[2].toUByte().toInt()
    val p2 = tx[3].toUByte().toInt()
    val p3 = tx[4].toUByte().toInt()
    val p4 = tx.drop(5).toByteArray().encodeHex()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        iccTransmitApduLogicalChannelByPort(
            slotIndex, portIndex, channel,
            cla, ins, p1, p2, p3, p4
        )
    } else {
        iccTransmitApduLogicalChannelBySlot(
            slotIndex, channel,
            cla, ins, p1, p2, p3, p4
        )
    }
}
