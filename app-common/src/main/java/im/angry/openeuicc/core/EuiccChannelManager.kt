package im.angry.openeuicc.core

import android.hardware.usb.UsbDevice

/**
 * EuiccChannelManager holds references to, and manages the lifecycles of, individual
 * APDU channels to SIM cards. The find* methods will create channels when needed, and
 * all opened channels will be held in an internal cache until invalidate() is called
 * or when this instance is destroyed.
 *
 * To precisely control the lifecycle of this object itself (and thus its cached channels),
 * all other compoents must access EuiccChannelManager objects through EuiccChannelManagerService.
 * Holding references independent of EuiccChannelManagerService is unsupported.
 */
interface EuiccChannelManager {
    companion object {
        const val USB_CHANNEL_ID = 99
    }

    /**
     * Scan all possible _device internal_ sources for EuiccChannels, return them and have all
     * scanned channels cached; these channels will remain open for the entire lifetime of
     * this EuiccChannelManager object, unless disconnected externally or invalidate()'d
     */
    suspend fun enumerateEuiccChannels(): List<EuiccChannel>

    /**
     * Scan all possible USB devices for CCID readers that may contain eUICC cards.
     * If found, try to open it for access, and add it to the internal EuiccChannel cache
     * as a "port" with id 99. When user interaction is required to obtain permission
     * to interact with the device, the second return value (EuiccChannel) will be null.
     */
    suspend fun enumerateUsbEuiccChannel(): Pair<UsbDevice?, EuiccChannel?>

    /**
     * Wait for a slot + port to reconnect (i.e. become valid again)
     * If the port is currently valid, this function will return immediately.
     * On timeout, the caller can decide to either try again later, or alert the user with an error
     */
    suspend fun waitForReconnect(physicalSlotId: Int, portId: Int, timeoutMillis: Long = 1000)

    /**
     * Returns the EuiccChannel corresponding to a **logical** slot
     */
    fun findEuiccChannelBySlotBlocking(logicalSlotId: Int): EuiccChannel?

    /**
     * Returns the first EuiccChannel corresponding to a **physical** slot
     * If the physical slot supports MEP and has multiple ports, it is undefined
     * which of the two channels will be returned.
     */
    fun findEuiccChannelByPhysicalSlotBlocking(physicalSlotId: Int): EuiccChannel?

    /**
     * Returns all EuiccChannels corresponding to a **physical** slot
     * Multiple channels are possible in the case of MEP
     */
    suspend fun findAllEuiccChannelsByPhysicalSlot(physicalSlotId: Int): List<EuiccChannel>?
    fun findAllEuiccChannelsByPhysicalSlotBlocking(physicalSlotId: Int): List<EuiccChannel>?

    /**
     * Returns the EuiccChannel corresponding to a **physical** slot and a port ID
     */
    suspend fun findEuiccChannelByPort(physicalSlotId: Int, portId: Int): EuiccChannel?
    fun findEuiccChannelByPortBlocking(physicalSlotId: Int, portId: Int): EuiccChannel?

    /**
     * Invalidate all EuiccChannels previously cached by this Manager
     */
    fun invalidate()

    /**
     * If possible, trigger the system to update the cached list of profiles
     * This is only expected to be implemented when the application is privileged
     * TODO: Remove this from the common interface
     */
    fun notifyEuiccProfilesChanged(logicalSlotId: Int) {
        // no-op by default
    }
}