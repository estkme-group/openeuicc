package im.angry.openeuicc.di

import android.content.Context
import im.angry.easyeuicc.R
import im.angry.openeuicc.core.EuiccChannel

class UnprivilegedCustomizableTextProvider(private val context: Context) :
    DefaultCustomizableTextProvider(context) {
    override fun formatNonUsbChannelName(logicalSlotId: Int): String =
        context.getString(R.string.channel_name_format_unpriv, logicalSlotId)

    override fun formatNonUsbChannelNameWithSeId(
        logicalSlotId: Int,
        seId: EuiccChannel.SecureElementId
    ): String =
        context.getString(R.string.channel_name_format_unpriv_se, logicalSlotId, seId.id)
}