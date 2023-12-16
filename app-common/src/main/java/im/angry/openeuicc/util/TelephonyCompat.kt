package im.angry.openeuicc.util

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.UiccCardInfo
import android.telephony.UiccPortInfo
import im.angry.openeuicc.util.*
import java.lang.RuntimeException

@Suppress("DEPRECATION")
class UiccCardInfoCompat(val inner: UiccCardInfo) {
    val physicalSlotIndex: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.physicalSlotIndex
            } else {
                inner.slotIndex
            }

    val ports: Collection<UiccPortInfoCompat>
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.ports.map { UiccPortInfoCompat(it, this) }
            } else {
                listOf(UiccPortInfoCompat(null, this))
            }

    val isEuicc: Boolean
        get() = inner.isEuicc

    val isRemovable: Boolean
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

class UiccPortInfoCompat(private val _inner: Any?, val card: UiccCardInfoCompat) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            check(_inner != null && _inner is UiccPortInfo) {
                "_inner is not UiccPortInfo on TIRAMISU"
            }
        }
    }

    val inner: UiccPortInfo
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                _inner as UiccPortInfo
            } else {
                throw RuntimeException("UiccPortInfo does not exist before T")
            }

    val portIndex: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.portIndex
            } else {
                0
            }

    val logicalSlotIndex: Int
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inner.logicalSlotIndex
            } else {
                card.physicalSlotIndex // logical is the same as physical below TIRAMISU
            }
}

val TelephonyManager.uiccCardsInfoCompat: List<UiccCardInfoCompat>
    @SuppressLint("MissingPermission")
    get() = uiccCardsInfo.map { UiccCardInfoCompat(it) }