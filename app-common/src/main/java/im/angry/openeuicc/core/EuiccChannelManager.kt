package im.angry.openeuicc.core

import android.content.Context
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

open class EuiccChannelManager(protected val context: Context) : IEuiccChannelManager {
    companion object {
        const val TAG = "EuiccChannelManager"
    }

    private val channels = mutableListOf<EuiccChannel>()

    private var seService: SEService? = null

    private val lock = Mutex()

    protected val tm by lazy {
        (context.applicationContext as OpenEuiccApplication).telephonyManager
    }

    protected open val uiccCards: Collection<UiccCardInfoCompat>
        get() = (0..<tm.activeModemCountCompat).map { FakeUiccCardInfoCompat(it) }

    private suspend fun ensureSEService() {
        if (seService == null) {
            seService = connectSEService(context)
        }
    }

    protected open fun tryOpenEuiccChannelPrivileged(port: UiccPortInfoCompat): EuiccChannel? {
        // No-op when unprivileged
        return null
    }

    protected fun tryOpenEuiccChannelUnprivileged(port: UiccPortInfoCompat): EuiccChannel? {
        if (port.portIndex != 0) {
            Log.w(TAG, "OMAPI channel attempted on non-zero portId, this may or may not work.")
        }

        Log.i(TAG, "Trying OMAPI for physical slot ${port.card.physicalSlotIndex}")
        try {
            return OmapiChannel(seService!!, port)
        } catch (e: IllegalArgumentException) {
            // Failed
            Log.w(
                TAG,
                "OMAPI APDU interface unavailable for physical slot ${port.card.physicalSlotIndex}."
            )
        }

        return null
    }

    private suspend fun tryOpenEuiccChannel(port: UiccPortInfoCompat): EuiccChannel? {
        lock.withLock {
            ensureSEService()
            val existing =
                channels.find { it.slotId == port.card.physicalSlotIndex && it.portId == port.portIndex }
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

    override fun findEuiccChannelBySlotBlocking(logicalSlotId: Int): EuiccChannel? =
        runBlocking {
            withContext(Dispatchers.IO) {
                for (card in uiccCards) {
                    for (port in card.ports) {
                        if (port.logicalSlotIndex == logicalSlotId) {
                            return@withContext tryOpenEuiccChannel(port)
                        }
                    }
                }

                null
            }
        }

    override fun findEuiccChannelByPhysicalSlotBlocking(physicalSlotId: Int): EuiccChannel? =
        runBlocking {
            withContext(Dispatchers.IO) {
                for (card in uiccCards) {
                    if (card.physicalSlotIndex != physicalSlotId) continue
                    for (port in card.ports) {
                        tryOpenEuiccChannel(port)?.let { return@withContext it }
                    }
                }

                null
            }
        }

    override fun findAllEuiccChannelsByPhysicalSlotBlocking(physicalSlotId: Int): List<EuiccChannel>? =
        runBlocking {
            for (card in uiccCards) {
                if (card.physicalSlotIndex != physicalSlotId) continue
                return@runBlocking card.ports.mapNotNull { tryOpenEuiccChannel(it) }
                    .ifEmpty { null }
            }
            return@runBlocking null
        }

    override fun findEuiccChannelByPortBlocking(physicalSlotId: Int, portId: Int): EuiccChannel? =
        runBlocking {
            withContext(Dispatchers.IO) {
                uiccCards.find { it.physicalSlotIndex == physicalSlotId }?.let { card ->
                    card.ports.find { it.portIndex == portId }?.let { tryOpenEuiccChannel(it) }
                }
            }
        }

    override suspend fun enumerateEuiccChannels() {
        withContext(Dispatchers.IO) {
            ensureSEService()

            for (uiccInfo in uiccCards) {
                for (port in uiccInfo.ports) {
                    if (tryOpenEuiccChannel(port) != null) {
                        Log.d(
                            TAG,
                            "Found eUICC on slot ${uiccInfo.physicalSlotIndex} port ${port.portIndex}"
                        )
                    }
                }
            }
        }
    }

    override val knownChannels: List<EuiccChannel>
        get() = channels.toList()

    override fun invalidate() {
        for (channel in channels) {
            channel.close()
        }

        channels.clear()
        seService?.shutdown()
        seService = null
    }
}