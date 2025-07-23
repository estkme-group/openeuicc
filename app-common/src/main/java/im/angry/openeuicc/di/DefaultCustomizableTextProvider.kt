package im.angry.openeuicc.di

import android.content.Context
import im.angry.openeuicc.common.R

open class DefaultCustomizableTextProvider(private val context: Context) : CustomizableTextProvider {
    override val noEuiccExplanation: String
        get() = context.getString(R.string.no_euicc)

    override val profileSwitchingTimeoutMessage: String
        get() = context.getString(R.string.profile_switch_timeout)

    override fun formatInternalChannelName(logicalSlotId: Int): String =
        context.getString(R.string.channel_name_format, logicalSlotId)
}