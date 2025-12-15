package im.angry.openeuicc.core

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.telephony.SubscriptionManager
import android.util.Log
import im.angry.openeuicc.core.usb.UsbCcidContext
import im.angry.openeuicc.core.usb.interfaces
import im.angry.openeuicc.core.usb.smartCard
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.util.FakeUiccCardInfoCompat
import im.angry.openeuicc.util.UiccCardInfoCompat
import im.angry.openeuicc.util.UiccPortInfoCompat
import im.angry.openeuicc.util.VendorAidDecider
import im.angry.openeuicc.util.activeModemCountCompat
import im.angry.openeuicc.util.parseIsdrAidList
import im.angry.openeuicc.util.queryVendorAidListTransformation
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

    private suspend inline fun tryOpenChannelWithKnownAids(
        supportsMultiSE: Boolean,
        openFn: (ByteArray, EuiccChannel.SecureElementId) -> EuiccChannel?
    ): List<EuiccChannel> {
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

                    // This only exists because the UI side doesn't yet support multi-SE over USB readers properly.
                    // TODO: Fix that and remove this.
                    if (!supportsMultiSE) {
                        break@outer
                    }
                }
            }

            // If we get here we should exit, since the inner loop completed without resetting
            break
        }

        // Set the hasMultipleSE field now since we only get to know that after we have iterated all AIDs
        // This also flips a flag in EuiccChannelImpl and prevents the field from being set again
        ret.forEach { it.hasMultipleSE = (seId > 1) }

        return ret
    }

    private suspend fun tryOpenEuiccChannel(
        port: UiccPortInfoCompat,
    ): List<EuiccChannel>? {
        lock.withLock {
            if (port.card.physicalSlotIndex == EuiccChannelManager.USB_CHANNEL_ID) {
                return usbChannels
            }

            // First get all channels for the requested port
            val existing =
                channelCache.filter { it.slotId == port.card.physicalSlotIndex && it.portId == port.portIndex }
            if (existing.isNotEmpty()) {
                if (existing.all { it.valid && it.logicalSlotId == port.logicalSlotIndex }) {
                    return existing
                } else {
                    // If any channel shouldn't be considered valid anymore, close all existing for the same slot / port
                    // and reopen
                    existing.forEach {
                        it.close()
                        channelCache.remove(it)
                    }
                }
            }

            if (port.logicalSlotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                // We can only open channels on ports that are actually enabled
                return null
            }

            // This function is not responsible for managing USB channels (see the initial check), so supportsMultiSE is true.
            val channels =
                tryOpenChannelWithKnownAids(supportsMultiSE = true) { isdrAid, seId ->
                    euiccChannelFactory.tryOpenEuiccChannel(
                        port,
                        isdrAid,
                        seId
                    )
                }

            if (channels.isNotEmpty()) {
                channelCache.addAll(channels)
                return channels
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
        seId: EuiccChannel.SecureElementId
    ): EuiccChannel? =
        withContext(Dispatchers.IO) {
            if (logicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                return@withContext usbChannels.find { it.seId == seId }
            }

            for (card in uiccCards) {
                for (port in card.ports) {
                    if (port.logicalSlotIndex == logicalSlotId) {
                        return@withContext tryOpenEuiccChannel(port)?.find { it.seId == seId }
                    }
                }
            }

            null
        }

    /**
     * Find all EuiccChannels associated with a _physical_ slot, including all secure elements
     * on cards with multiple of them.
     */
    private suspend fun findAllEuiccChannelsByPhysicalSlot(physicalSlotId: Int): List<EuiccChannel>? {
        if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
            return usbChannels.ifEmpty { null }
        }

        for (card in uiccCards) {
            if (card.physicalSlotIndex != physicalSlotId) continue
            return card.ports.mapNotNull { tryOpenEuiccChannel(it) }
                .flatten()
                .ifEmpty { null }
        }
        return null
    }

    /**
     * Finds all EuiccChannels associated with a physical slot + port. Note that this
     * may return multiple in case there are multiple SEs.
     */
    private suspend fun findEuiccChannelsByPort(
        physicalSlotId: Int,
        portId: Int,
    ): List<EuiccChannel>? =
        withContext(Dispatchers.IO) {
            if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                return@withContext usbChannels.ifEmpty { null }
            }

            uiccCards.find { it.physicalSlotIndex == physicalSlotId }?.let { card ->
                card.ports.find { it.portIndex == portId }?.let { tryOpenEuiccChannel(it) }
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
        val channel = findEuiccChannelsByPort(physicalSlotId, portId)?.find { it.seId == seId }
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
        val numChannelsBefore = if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
            usbChannels.size
        } else {
            // Don't use find* methods since they reopen channels if not found
            channelCache.filter { it.slotId == physicalSlotId && it.portId == portId }.size
        }

        val resetChannels = {
            if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                usbChannels.forEach { it.close() }
                usbChannels.clear()
            } else {
                // If there is already a valid channel, we close it proactively
                channelCache.filter { it.slotId == physicalSlotId && it.portId == portId }.forEach { it.close() }
            }
        }

        resetChannels()

        withTimeout(timeoutMillis) {
            while (true) {
                try {
                    val channels = if (physicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                        // tryOpenUsbEuiccChannel() will always try to reopen the channel, even if
                        // a USB channel already exists
                        tryOpenUsbEuiccChannel()
                        usbChannels
                    } else {
                        // tryOpenEuiccChannel() will automatically dispose of invalid channels
                        // and recreate when needed
                        findEuiccChannelsByPort(physicalSlotId, portId)!!
                    }
                    check(channels.isNotEmpty()) { "No channel" }
                    check(channels.all { it.valid }) { "Invalid channel" }
                    check(numChannelsBefore > 0 && channels.size >= numChannelsBefore) { "Less channels than before" }
                    break
                } catch (e: Exception) {
                    Log.d(
                        TAG,
                        "Slot $physicalSlotId port $portId reconnect failure, retrying in 1000 ms"
                    )
                    resetChannels()
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
        findEuiccChannelsByPort(slotId, portId)?.forEach {
            emit(it.seId)
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
                    // TODO: We should also support multiple SEs over USB readers (the code here already does, UI doesn't yet)
                    val channels = tryOpenChannelWithKnownAids(supportsMultiSE = false) { isdrAid, seId ->
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
