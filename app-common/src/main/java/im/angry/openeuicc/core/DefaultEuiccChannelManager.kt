package im.angry.openeuicc.core

import android.content.Context
import android.telephony.SubscriptionManager
import android.util.Log
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

open class DefaultEuiccChannelManager(
    protected val appContainer: AppContainer,
    protected val context: Context
) : EuiccChannelManager {
    companion object {
        const val TAG = "EuiccChannelManager"
    }

    private val channels = mutableListOf<EuiccChannel>()

    private val lock = Mutex()

    protected val tm by lazy {
        appContainer.telephonyManager
    }

    private val euiccChannelFactory by lazy {
        appContainer.euiccChannelFactory
    }

    protected open val uiccCards: Collection<UiccCardInfoCompat>
        get() = (0..<tm.activeModemCountCompat).map { FakeUiccCardInfoCompat(it) }

    private suspend fun tryOpenEuiccChannel(port: UiccPortInfoCompat): EuiccChannel? {
        lock.withLock {
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

            return euiccChannelFactory.tryOpenEuiccChannel(port)?.also {
                channels.add(it)
            }
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
        euiccChannelFactory.cleanup()
    }
}