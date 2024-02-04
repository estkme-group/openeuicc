package im.angry.openeuicc.util

import android.content.Context
import android.os.Build
import android.se.omapi.Reader
import android.se.omapi.SEService
import android.telephony.TelephonyManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val TelephonyManager.activeModemCountCompat: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activeModemCount
    } else {
        phoneCount
    }

fun SEService.getUiccReaderCompat(slotNumber: Int): Reader {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return getUiccReader(slotNumber)
    } else {
        return readers.first { it.name == "SIM${slotNumber}" || (slotNumber == 1 && it.name == "SIM") }
    }
}

/*
 * In the privileged version, the EuiccChannelManager should work
 * based on real Uicc{Card,Port}Info reported by TelephonyManager.
 * However, when unprivileged, we cannot depend on the fact that
 * we can access TelephonyManager. ARA-M only grants access to
 * OMAPI, but not TelephonyManager APIs that are associated with
 * carrier privileges.
 *
 * To maximally share code between the two variants, we define
 * an interface of whatever information will be used in the shared
 * portion of EuiccChannelManager etc. When unprivileged, we
 * generate "fake" versions based solely on how many slots the phone
 * has, while the privileged version can populate the fields with
 * real information, extending whenever needed.
 */
interface UiccCardInfoCompat {
    val physicalSlotIndex: Int
    val ports: Collection<UiccPortInfoCompat>
}

interface UiccPortInfoCompat {
    val card: UiccCardInfoCompat
    val portIndex: Int
    val logicalSlotIndex: Int
}

data class FakeUiccCardInfoCompat(
    override val physicalSlotIndex: Int,
): UiccCardInfoCompat {
    override val ports: Collection<UiccPortInfoCompat> =
        listOf(FakeUiccPortInfoCompat(this))
}

data class FakeUiccPortInfoCompat(
    override val card: UiccCardInfoCompat
): UiccPortInfoCompat {
    override val portIndex: Int = 0
    override val logicalSlotIndex: Int = card.physicalSlotIndex
}