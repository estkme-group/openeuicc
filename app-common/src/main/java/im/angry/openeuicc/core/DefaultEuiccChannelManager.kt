package im.angry.openeuicc.core

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.telephony.SubscriptionManager
import android.util.Log
import im.angry.openeuicc.core.usb.getSmartCardInterface
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

open class DefaultEuiccChannelManager(
    protected val appContainer: AppContainer,
    protected val context: Context
) : EuiccChannelManager {
    companion object {
        const val TAG = "EuiccChannelManager"
    }

    private val channelCache = mutableListOf<EuiccChannel>()

    private var usbChannel: EuiccChannel? = null

    private val lock = Mutex()

    protected val tm by lazy {
        appContainer.telephonyManager
    }

    private val usbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    private val euiccChannelFactory by lazy {
        appContainer.euiccChannelFactory
    }

    protected open val uiccCards: Collection<UiccCardInfoCompat>
        get() = (0..<tm.activeModemCountCompat).map { FakeUiccCardInfoCompat(it) }

    private suspend fun tryOpenEuiccChannel(port: UiccPortInfoCompat): EuiccChannel? {
        lock.withLock {
            if (port.card.physicalSlotIndex == EuiccChannelManager.USB_CHANNEL_ID) {
                return if (usbChannel != null && usbChannel!!.valid) {
                    usbChannel
                } else {
                    usbChannel = null
                    null
                }
            }

            val existing =
                channelCache.find { it.slotId == port.card.physicalSlotIndex && it.portId == port.portIndex }
            if (existing != null) {
                if (existing.valid && port.logicalSlotIndex == existing.logicalSlotId) {
                    return existing
                } else {
                    existing.close()
                    channelCache.remove(existing)
                }
            }

            if (port.logicalSlotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                // We can only open channels on ports that are actually enabled
                return null
            }

            val channel = euiccChannelFactory.tryOpenEuiccChannel(port) ?: return null

            if (channel.valid) {
                channelCache.add(channel)
                return channel
            } else {
                Log.i(
                    TAG,
                    "Was able to open channel for logical slot ${port.logicalSlotIndex}, but the channel is invalid (cannot get eID or profiles without errors). This slot might be broken, aborting."
                )
                channel.close()
                return null
            }
        }
    }

    override fun findEuiccChannelBySlotBlocking(logicalSlotId: Int): EuiccChannel? =
        runBlocking {
            withContext(Dispatchers.IO) {
                if (logicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                    return@withContext usbChannel
                }

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
                if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                    return@withContext usbChannel
                }

                for (card in uiccCards) {
                    if (card.physicalSlotIndex != physicalSlotId) continue
                    for (port in card.ports) {
                        tryOpenEuiccChannel(port)?.let { return@withContext it }
                    }
                }

                null
            }
        }

    override suspend fun findAllEuiccChannelsByPhysicalSlot(physicalSlotId: Int): List<EuiccChannel>? {
        if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
            return usbChannel?.let { listOf(it) }
        }

        for (card in uiccCards) {
            if (card.physicalSlotIndex != physicalSlotId) continue
            return card.ports.mapNotNull { tryOpenEuiccChannel(it) }
                .ifEmpty { null }
        }
        return null
    }

    override fun findAllEuiccChannelsByPhysicalSlotBlocking(physicalSlotId: Int): List<EuiccChannel>? =
        runBlocking {
            findAllEuiccChannelsByPhysicalSlot(physicalSlotId)
        }

    override suspend fun findEuiccChannelByPort(physicalSlotId: Int, portId: Int): EuiccChannel? =
        withContext(Dispatchers.IO) {
            if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                return@withContext usbChannel
            }

            uiccCards.find { it.physicalSlotIndex == physicalSlotId }?.let { card ->
                card.ports.find { it.portIndex == portId }?.let { tryOpenEuiccChannel(it) }
            }
        }

    override fun findEuiccChannelByPortBlocking(physicalSlotId: Int, portId: Int): EuiccChannel? =
        runBlocking {
            findEuiccChannelByPort(physicalSlotId, portId)
        }

    override suspend fun waitForReconnect(physicalSlotId: Int, portId: Int, timeoutMillis: Long) {
        if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) return

        // If there is already a valid channel, we close it proactively
        // Sometimes the current channel can linger on for a bit even after it should have become invalid
        channelCache.find { it.slotId == physicalSlotId && it.portId == portId }?.apply {
            if (valid) close()
        }

        withTimeout(timeoutMillis) {
            while (true) {
                try {
                    // tryOpenEuiccChannel() will automatically dispose of invalid channels
                    // and recreate when needed
                    val channel = findEuiccChannelByPortBlocking(physicalSlotId, portId)!!
                    check(channel.valid) { "Invalid channel" }
                    break
                } catch (e: Exception) {
                    Log.d(TAG, "Slot $physicalSlotId port $portId reconnect failure, retrying in 1000 ms")
                }
                delay(1000)
            }
        }
    }

    override suspend fun enumerateEuiccChannels(): List<EuiccChannel> =
        withContext(Dispatchers.IO) {
            uiccCards.flatMap { info ->
                info.ports.mapNotNull { port ->
                    tryOpenEuiccChannel(port)?.also {
                        Log.d(
                            TAG,
                            "Found eUICC on slot ${info.physicalSlotIndex} port ${port.portIndex}"
                        )
                    }
                }
            }
        }

    override suspend fun enumerateUsbEuiccChannel(): Pair<UsbDevice?, EuiccChannel?> =
        withContext(Dispatchers.IO) {
            usbManager.deviceList.values.forEach { device ->
                Log.i(TAG, "Scanning USB device ${device.deviceId}:${device.vendorId}")
                val iface = device.getSmartCardInterface() ?: return@forEach
                // If we don't have permission, tell UI code that we found a candidate device, but we
                // need permission to be able to do anything with it
                if (!usbManager.hasPermission(device)) return@withContext Pair(device, null)
                Log.i(TAG, "Found CCID interface on ${device.deviceId}:${device.vendorId}, and has permission; trying to open channel")
                try {
                    val channel = euiccChannelFactory.tryOpenUsbEuiccChannel(device, iface)
                    if (channel != null && channel.lpa.valid) {
                        usbChannel = channel
                        return@withContext Pair(device, channel)
                    }
                } catch (e: Exception) {
                    // Ignored -- skip forward
                    e.printStackTrace()
                }
                Log.i(TAG, "No valid eUICC channel found on USB device ${device.deviceId}:${device.vendorId}")
            }
            return@withContext Pair(null, null)
        }

    override fun invalidate() {
        for (channel in channelCache) {
            channel.close()
        }

        usbChannel?.close()
        usbChannel = null
        channelCache.clear()
        euiccChannelFactory.cleanup()
    }
}