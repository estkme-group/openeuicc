package im.angry.openeuicc.core

interface EuiccChannelManager {
    val knownChannels: List<EuiccChannel>

    /**
     * Scan all possible sources for EuiccChannels and have them cached for future use
     */
    suspend fun enumerateEuiccChannels()

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
     * Invalidate all EuiccChannels previously known by this Manager
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