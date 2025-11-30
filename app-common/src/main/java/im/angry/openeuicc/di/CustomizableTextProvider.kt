package im.angry.openeuicc.di

import im.angry.openeuicc.core.EuiccChannel

interface CustomizableTextProvider {
    /**
     * Explanation string for when no eUICC is found on the device.
     * This could be different depending on whether the app is privileged or not.
     */
    val noEuiccExplanation: String

    /**
     * Shown when we timed out switching between profiles.
     */
    val profileSwitchingTimeoutMessage: String

    /**
     * Format the name of a logical slot -- not for USB channels
     */
    fun formatNonUsbChannelName(logicalSlotId: Int): String

    /**
     * Format the name of a logical slot with a SE ID, in case of multi-SE chips; currently
     * this is used in the download flow to distinguish between them on the same chip.
     */
    fun formatNonUsbChannelNameWithSeId(logicalSlotId: Int, seId: EuiccChannel.SecureElementId): String
}