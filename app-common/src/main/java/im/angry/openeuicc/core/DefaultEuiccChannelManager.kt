package im.angry.openeuicc.core

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.telephony.SubscriptionManager
import android.util.Log
import im.angry.openeuicc.core.usb.UsbCcidContext
import im.angry.openeuicc.core.usb.smartCard
import im.angry.openeuicc.core.usb.interfaces
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
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

    private var usbChannels = mutableListOf<EuiccChannel>()

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

    private suspend inline fun tryOpenChannelWithKnownAids(openFn: (ByteArray, EuiccChannel.SecureElementId) -> EuiccChannel?): List<EuiccChannel> {
        var isdrAidList =
            parseIsdrAidList(appContainer.preferenceRepository.isdrAidListFlow.first())
        val ret = mutableListOf<EuiccChannel>()
        val openedAids = mutableListOf<ByteArray>()
        var hasReset = false
        var vendorDecider: VendorAidDecider? = null
        var seId = 0

        outer@ while (true) {
            for (aid in isdrAidList) {
                if (vendorDecider != null && !vendorDecider.shouldOpenMore(openedAids, aid)) {
                    break@outer
                }

                val channel =
                    openFn(aid, EuiccChannel.SecureElementId.createFromInt(seId))?.let { channel ->
                        if (channel.valid) {
                            seId += 1
                            channel
                        } else {
                            channel.close()
                            null
                        }
                    }

                if (!hasReset) {
                    val res = channel?.queryVendorAidListTransformation(isdrAidList)
                    if (res != null) {
                        // Reset the for loop since we needed to replace the AID list due to vendor-specific code
                        Log.i(TAG, "AID list replaced, resetting open attempt")
                        isdrAidList = res.first
                        vendorDecider = res.second
                        seId = 0
                        ret.clear()
                        openedAids.clear()
                        channel.close()
                        hasReset = true // Don't let anything reset again
                        continue@outer
                    }
                }

                if (channel != null) {
                    ret.add(channel)
                    openedAids.add(aid)
                }
            }

            // If we get here we should exit, since the inner loop completed without resetting
            break
        }

        return ret
    }

    private suspend fun tryOpenEuiccChannel(
        port: UiccPortInfoCompat,
        seId: EuiccChannel.SecureElementId = EuiccChannel.SecureElementId.DEFAULT
    ): EuiccChannel? {
        lock.withLock {
            if (port.card.physicalSlotIndex == EuiccChannelManager.USB_CHANNEL_ID) {
                // We only compare seId because we assume we can only open 1 card from USB
                return usbChannels.find { it.seId == seId }
            }

            val existing =
                channelCache.find { it.slotId == port.card.physicalSlotIndex && it.portId == port.portIndex && it.seId == seId }
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

            val channels =
                tryOpenChannelWithKnownAids { isdrAid, seId ->
                    euiccChannelFactory.tryOpenEuiccChannel(
                        port,
                        isdrAid,
                        seId
                    )
                }

            if (channels.isNotEmpty()) {
                channelCache.addAll(channels)
                return channels.find { it.seId == seId }
            } else {
                Log.i(
                    TAG,
                    "Was able to open channel for logical slot ${port.logicalSlotIndex}, but the channel is invalid (cannot get eID or profiles without errors). This slot might be broken, aborting."
                )
                return null
            }
        }
    }

    protected suspend fun findEuiccChannelByLogicalSlot(
        logicalSlotId: Int,
        seId: EuiccChannel.SecureElementId = EuiccChannel.SecureElementId.DEFAULT
    ): EuiccChannel? =
        withContext(Dispatchers.IO) {
            if (logicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                return@withContext usbChannels.find { it.seId == seId }
            }

            for (card in uiccCards) {
                for (port in card.ports) {
                    if (port.logicalSlotIndex == logicalSlotId) {
                        return@withContext tryOpenEuiccChannel(port, seId)
                    }
                }
            }

            null
        }

    private suspend fun findAllEuiccChannelsByPhysicalSlot(physicalSlotId: Int): List<EuiccChannel>? {
        if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
            return usbChannels.ifEmpty { null }
        }

        for (card in uiccCards) {
            if (card.physicalSlotIndex != physicalSlotId) continue
            return card.ports.mapNotNull { tryOpenEuiccChannel(it) }
                .ifEmpty { null }
        }
        return null
    }

    private suspend fun findEuiccChannelByPort(
        physicalSlotId: Int,
        portId: Int,
        seId: EuiccChannel.SecureElementId = EuiccChannel.SecureElementId.DEFAULT
    ): EuiccChannel? =
        withContext(Dispatchers.IO) {
            if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                return@withContext usbChannels.find { it.seId == seId }
            }

            uiccCards.find { it.physicalSlotIndex == physicalSlotId }?.let { card ->
                card.ports.find { it.portIndex == portId }?.let { tryOpenEuiccChannel(it, seId) }
            }
        }

    override suspend fun findFirstAvailablePort(physicalSlotId: Int): Int =
        withContext(Dispatchers.IO) {
            if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                return@withContext 0
            }

            findAllEuiccChannelsByPhysicalSlot(physicalSlotId)?.getOrNull(0)?.portId ?: -1
        }

    override suspend fun findAvailablePorts(physicalSlotId: Int): List<Int> =
        withContext(Dispatchers.IO) {
            if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                return@withContext listOf(0)
            }

            findAllEuiccChannelsByPhysicalSlot(physicalSlotId)?.map { it.portId }?.toSet()?.toList()
                ?: listOf()
        }

    override suspend fun <R> withEuiccChannel(
        physicalSlotId: Int,
        portId: Int,
        seId: EuiccChannel.SecureElementId,
        fn: suspend (EuiccChannel) -> R
    ): R {
        val channel = findEuiccChannelByPort(physicalSlotId, portId, seId)
            ?: throw EuiccChannelManager.EuiccChannelNotFoundException()
        val wrapper = EuiccChannelWrapper(channel)
        try {
            return withContext(Dispatchers.IO) {
                fn(wrapper)
            }
        } finally {
            wrapper.invalidateWrapper()
        }
    }

    override suspend fun <R> withEuiccChannel(
        logicalSlotId: Int,
        seId: EuiccChannel.SecureElementId,
        fn: suspend (EuiccChannel) -> R
    ): R {
        val channel = findEuiccChannelByLogicalSlot(logicalSlotId, seId)
            ?: throw EuiccChannelManager.EuiccChannelNotFoundException()
        val wrapper = EuiccChannelWrapper(channel)
        try {
            return withContext(Dispatchers.IO) {
                fn(wrapper)
            }
        } finally {
            wrapper.invalidateWrapper()
        }
    }

    override suspend fun waitForReconnect(physicalSlotId: Int, portId: Int, timeoutMillis: Long) {
        if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
            usbChannels.forEach { it.close() }
            usbChannels.clear()
        } else {
            // If there is already a valid channel, we close it proactively
            // Sometimes the current channel can linger on for a bit even after it should have become invalid
            channelCache.find { it.slotId == physicalSlotId && it.portId == portId }?.apply {
                if (valid) close()
            }
        }

        withTimeout(timeoutMillis) {
            while (true) {
                try {
                    val channel = if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                        // tryOpenUsbEuiccChannel() will always try to reopen the channel, even if
                        // a USB channel already exists
                        tryOpenUsbEuiccChannel()
                        usbChannels.getOrNull(0)!!
                    } else {
                        // tryOpenEuiccChannel() will automatically dispose of invalid channels
                        // and recreate when needed
                        findEuiccChannelByPort(physicalSlotId, portId)!!
                    }
                    check(channel.valid) { "Invalid channel" }
                    break
                } catch (e: Exception) {
                    Log.d(
                        TAG,
                        "Slot $physicalSlotId port $portId reconnect failure, retrying in 1000 ms"
                    )
                }
                delay(1000)
            }
        }
    }

    override fun flowInternalEuiccPorts(): Flow<Pair<Int, Int>> = flow {
        uiccCards.forEach { info ->
            info.ports.forEach { port ->
                tryOpenEuiccChannel(port)?.also {
                    Log.d(
                        TAG,
                        "Found eUICC on slot ${info.physicalSlotIndex} port ${port.portIndex}"
                    )

                    emit(Pair(info.physicalSlotIndex, port.portIndex))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun flowAllOpenEuiccPorts(): Flow<Pair<Int, Int>> =
        merge(flowInternalEuiccPorts(), flow {
            if (tryOpenUsbEuiccChannel().second) {
                emit(Pair(EuiccChannelManager.USB_CHANNEL_ID, 0))
            }
        })

    override fun flowEuiccSecureElements(
        slotId: Int,
        portId: Int
    ): Flow<EuiccChannel.SecureElementId> = flow {
        // Emit the "default" channel first
        // TODO: This function below should really return a list, not just one SE
        findEuiccChannelByPort(slotId, portId, seId = EuiccChannel.SecureElementId.DEFAULT)?.let {
            emit(EuiccChannel.SecureElementId.DEFAULT)

            channelCache.filter { it.slotId == slotId && it.portId == portId && it.seId != EuiccChannel.SecureElementId.DEFAULT }
                .forEach { emit(it.seId) }
        }
    }

    override suspend fun tryOpenUsbEuiccChannel(): Pair<UsbDevice?, Boolean> =
        withContext(Dispatchers.IO) {
            usbManager.deviceList.values.forEach { device ->
                Log.i(TAG, "Scanning USB device ${device.deviceId}:${device.vendorId}")
                val iface = device.interfaces.smartCard ?: return@forEach
                // If we don't have permission, tell UI code that we found a candidate device, but we
                // need permission to be able to do anything with it
                if (!usbManager.hasPermission(device)) return@withContext Pair(device, false)
                Log.i(
                    TAG,
                    "Found CCID interface on ${device.deviceId}:${device.vendorId}, and has permission; trying to open channel"
                )

                val ccidCtx =
                    UsbCcidContext.createFromUsbDevice(context, device, iface) ?: return@forEach

                try {
                    val channels = tryOpenChannelWithKnownAids { isdrAid, seId ->
                        euiccChannelFactory.tryOpenUsbEuiccChannel(ccidCtx, isdrAid, seId)
                    }
                    if (channels.isNotEmpty() && channels[0].valid) {
                        ccidCtx.allowDisconnect = true
                        usbChannels.clear()
                        usbChannels.addAll(channels)
                        return@withContext Pair(device, true)
                    }
                } catch (e: Exception) {
                    // Ignored -- skip forward
                    e.printStackTrace()
                }

                ccidCtx.allowDisconnect = true
                ccidCtx.disconnect()

                Log.i(
                    TAG,
                    "No valid eUICC channel found on USB device ${device.deviceId}:${device.vendorId}"
                )
            }
            return@withContext Pair(null, false)
        }

    override fun invalidate() {
        for (channel in channelCache) {
            channel.close()
        }

        usbChannels.forEach { it.close() }
        usbChannels.clear()
        channelCache.clear()
        euiccChannelFactory.cleanup()
    }
}