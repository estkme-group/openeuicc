package im.angry.openeuicc.di

interface CustomizableTextProvider {
    /**
     * Format the name of a logical slot; internal only -- not intended for
     * other channels such as USB.
     */
    fun formatInternalChannelName(logicalSlotId: Int): String
}