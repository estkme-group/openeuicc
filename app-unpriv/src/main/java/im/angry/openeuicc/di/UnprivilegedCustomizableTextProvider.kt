package im.angry.openeuicc.di

import android.content.Context
import im.angry.easyeuicc.R

class UnprivilegedCustomizableTextProvider(private val context: Context) :
    DefaultCustomizableTextProvider(context) {
    override fun formatInternalChannelName(logicalSlotId: Int): String =
        context.getString(R.string.channel_name_format_unpriv, logicalSlotId)
}