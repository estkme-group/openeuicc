package im.angry.openeuicc.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.se.omapi.SEService
import android.telephony.SubscriptionManager
import android.util.Log
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class EuiccChannelManager(protected val context: Context) {
    companion object {
        const val TAG = "EuiccChannelManager"
    }

    private val channels = mutableListOf<EuiccChannel>()

    private var seService: SEService? = null

    private val lock = Mutex()

    protected val tm by lazy {
        (context.applicationContext as OpenEuiccApplication).telephonyManager
    }

    private val handler = Handler(HandlerThread("BaseEuiccChannelManager").also { it.start() }.looper)

    protected open fun checkPrivileges() = tm.hasCarrierPrivileges()

    private suspend fun connectSEService(): SEService = suspendCoroutine { cont ->
        handler.post {
            var service: SEService? = null
            service = SEService(context, { handler.post(it) }) {
                cont.resume(service!!)
            }
        }
    }

    private suspend fun ensureSEService() {
         if (seService == null) {
             seService = connectSEService()
         }
    }

    protected open fun tryOpenEuiccChannelPrivileged(port: UiccPortInfoCompat): EuiccChannel? {
        // No-op when unprivileged
        return null
    }

    protected fun tryOpenEuiccChannelUnprivileged(port: UiccPortInfoCompat): EuiccChannel? {
        if (port.portIndex != 0) {
            Log.w(TAG, "OMAPI channel attempted on non-zero portId, ignoring")
            return null
        }

        Log.i(TAG, "Trying OMAPI for physical slot ${port.card.physicalSlotIndex}")
        try {
            return OmapiChannel(seService!!, port)
        } catch (e: IllegalArgumentException) {
            // Failed
            Log.w(TAG, "OMAPI APDU interface unavailable for physical slot ${port.card.physicalSlotIndex}.")
        }

        return null
    }

    private suspend fun tryOpenEuiccChannel(port: UiccPortInfoCompat): EuiccChannel? {
        lock.withLock {
            ensureSEService()
            val existing = channels.find { it.slotId == port.card.physicalSlotIndex && it.portId == port.portIndex }
            if (existing != null) {
                if (existing.valid && port.logicalSlotIndex == existing.logicalSlotId) {
                    return existing
                } else {
                    existing.close()
                    channels.remove(existing)
                }
            }

            if (port.logicalSlotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                // We can only open channels on ports that are actually enabled
                return null
            }

            var euiccChannel: EuiccChannel? = tryOpenEuiccChannelPrivileged(port)

            if (euiccChannel == null) {
                euiccChannel = tryOpenEuiccChannelUnprivileged(port)
            }

            if (euiccChannel != null) {
                channels.add(euiccChannel)
            }

            return euiccChannel
        }
    }

    fun findEuiccChannelBySlotBlocking(logicalSlotId: Int): EuiccChannel? =
        runBlocking {
            if (!checkPrivileges()) return@runBlocking null
            withContext(Dispatchers.IO) {
                for (card in tm.uiccCardsInfoCompat) {
                    for (port in card.ports) {
                        if (port.logicalSlotIndex == logicalSlotId) {
                            return@withContext tryOpenEuiccChannel(port)
                        }
                    }
                }

                null
            }
        }

    fun findEuiccChannelByPortBlocking(physicalSlotId: Int, portId: Int): EuiccChannel? = runBlocking {
        if (!checkPrivileges()) return@runBlocking null
        withContext(Dispatchers.IO) {
            tm.uiccCardsInfoCompat.find { it.physicalSlotIndex == physicalSlotId }?.let { card ->
                card.ports.find { it.portIndex == portId }?.let { tryOpenEuiccChannel(it) }
            }
        }
    }

    suspend fun enumerateEuiccChannels() {
        if (!checkPrivileges()) return

        withContext(Dispatchers.IO) {
            ensureSEService()

            for (uiccInfo in tm.uiccCardsInfoCompat) {
                for (port in uiccInfo.ports) {
                    if (tryOpenEuiccChannel(port) != null) {
                        Log.d(TAG, "Found eUICC on slot ${uiccInfo.physicalSlotIndex} port ${port.portIndex}")
                    }
                }
            }
        }
    }

    val knownChannels: List<EuiccChannel>
        get() = channels.toList()

    fun invalidate() {
        if (!checkPrivileges()) return

        for (channel in channels) {
            channel.close()
        }

        channels.clear()
        seService?.shutdown()
        seService = null
    }

    open fun notifyEuiccProfilesChanged(logicalSlotId: Int) {
        // No-op for unprivileged
    }
}