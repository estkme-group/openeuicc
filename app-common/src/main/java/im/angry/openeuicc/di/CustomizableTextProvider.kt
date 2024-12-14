package im.angry.openeuicc.di

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
     * Format the name of a logical slot; internal only -- not intended for
     * other channels such as USB.
     */
    fun formatInternalChannelName(logicalSlotId: Int): String
}