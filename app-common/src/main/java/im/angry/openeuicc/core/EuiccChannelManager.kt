package im.angry.openeuicc.core

import android.hardware.usb.UsbDevice
import kotlinx.coroutines.flow.Flow

/**
 * EuiccChannelManager holds references to, and manages the lifecycles of, individual
 * APDU channels to SIM cards. The find* methods will create channels when needed, and
 * all opened channels will be held in an internal cache until invalidate() is called
 * or when this instance is destroyed.
 *
 * To precisely control the lifecycle of this object itself (and thus its cached channels),
 * all other components must access EuiccChannelManager objects through EuiccChannelManagerService.
 * Holding references independent of EuiccChannelManagerService is unsupported.
 */
interface EuiccChannelManager {
    companion object {
        const val USB_CHANNEL_ID = 99
    }

    /**
     * Scan all possible _device internal_ sources for EuiccChannels, as a flow, return their physical
     * (slotId, portId) and have all scanned channels cached; these channels will remain open
     * for the entire lifetime of this EuiccChannelManager object, unless disconnected externally
     * or invalidate()'d.
     *
     * To obtain a temporary reference to a EuiccChannel, use `withEuiccChannel()`.
     */
    fun flowInternalEuiccPorts(): Flow<Pair<Int, Int>>

    /**
     * Same as flowInternalEuiccPorts(), except that this includes non-device internal eUICC chips
     * as well. Namely, this includes the USB reader.
     *
     * Non-internal readers will only be included if they have been opened properly, i.e. with permissions
     * granted by the user.
     */
    fun flowAllOpenEuiccPorts(): Flow<Pair<Int, Int>>

    /**
     * Iterate over all the Secure Elements available on one eUICC.
     *
     * This is going to almost always return only 1 result, except in the case where
     * a card has multiple SEs.
     */
    fun flowEuiccSecureElements(slotId: Int, portId: Int): Flow<EuiccChannel.SecureElementId>

    /**
     * Scan all possible USB devices for CCID readers that may contain eUICC cards.
     * If found, try to open it for access, and add it to the internal EuiccChannel cache
     * as a "port" with id 99. When user interaction is required to obtain permission
     * to interact with the device, the second return value will be false.
     *
     * Returns (usbDevice, canOpen). canOpen is false if either (1) no usb reader is found;
     * or (2) usb reader is found, but user interaction is required for access;
     * or (3) usb reader is found, but we are unable to open ISD-R.
     */
    suspend fun tryOpenUsbEuiccChannel(): Pair<UsbDevice?, Boolean>

    /**
     * Wait for a slot + port to reconnect (i.e. become valid again)
     * If the port is currently valid, this function will return immediately.
     * On timeout, the caller can decide to either try again later, or alert the user with an error
     */
    suspend fun waitForReconnect(physicalSlotId: Int, portId: Int, timeoutMillis: Long = 1000)

    /**
     * Returns the first mapped & available port ID for a physical slot, or -1 if
     * not found.
     */
    suspend fun findFirstAvailablePort(physicalSlotId: Int): Int

    /**
     * Returns all mapped & available port IDs for a physical slot.
     */
    suspend fun findAvailablePorts(physicalSlotId: Int): List<Int>

    class EuiccChannelNotFoundException : Exception("EuiccChannel not found")

    /**
     * Find a EuiccChannel by its slot and port, then run a callback with a reference to it.
     * The reference is not supposed to be held outside of the callback. This is enforced via
     * a wrapper object.
     *
     * The callback is run on Dispatchers.IO by default.
     *
     * If a channel for that slot / port is not found, EuiccChannelNotFoundException is thrown
     */
    suspend fun <R> withEuiccChannel(physicalSlotId: Int, portId: Int, seId: EuiccChannel.SecureElementId, fn: suspend (EuiccChannel) -> R): R

    /**
     * Same as withEuiccChannel(Int, Int, SecureElementId, (EuiccChannel) -> R) but instead uses logical slot ID
     */
    suspend fun <R> withEuiccChannel(logicalSlotId: Int, seId: EuiccChannel.SecureElementId, fn: suspend (EuiccChannel) -> R): R

    /**
     * Invalidate all EuiccChannels previously cached by this Manager
     */
    fun invalidate()

    /**
     * If possible, trigger the system to update the cached list of profiles
     * This is only expected to be implemented when the application is privileged
     * TODO: Remove this from the common interface
     */
    suspend fun notifyEuiccProfilesChanged(logicalSlotId: Int) {
        // no-op by default
    }
}
