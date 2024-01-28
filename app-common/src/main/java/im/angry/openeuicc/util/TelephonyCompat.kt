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

// Create an instance of OMAPI SEService in a manner that "makes sense" without unpredictable callbacks
suspend fun connectSEService(context: Context): SEService = suspendCoroutine { cont ->
    // Use a Mutex to make sure the continuation is run *after* the "service" variable is assigned
    val lock = Mutex()
    var service: SEService? = null
    val callback = {
        runBlocking {
            lock.withLock {
                cont.resume(service!!)
            }
        }
    }

    runBlocking {
        // If this were not protected by a Mutex, callback might be run before service is even assigned
        // Yes, we are on Android, we could have used something like a Handler, but we cannot really
        // assume the coroutine is run on a thread that has a Handler. We either use our own HandlerThread
        // (and then cleanup becomes an issue), or we use a lock
        lock.withLock {
            try {
                service = SEService(context, { it.run() }, callback)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
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