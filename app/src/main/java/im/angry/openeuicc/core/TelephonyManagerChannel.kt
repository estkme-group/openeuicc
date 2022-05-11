package im.angry.openeuicc.core

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.TelephonyManager
import android.util.Log
import com.truphone.lpa.LocalProfileAssistant
import com.truphone.lpa.impl.LocalProfileAssistantImpl
import im.angry.openeuicc.util.*
import java.lang.Exception

class TelephonyManagerChannel private constructor(
    info: EuiccChannelInfo,
    private val tm: TelephonyManager,
    private val channelId: Int
) : EuiccChannel(info) {
    companion object {
        private const val TAG = "TelephonyManagerApduChannel"
        private const val EUICC_APP_ID = "A0000005591010FFFFFFFF8900000100"

        // TODO: On Tiramisu, we need to specify the portId also if we want MEP support
        fun tryConnect(tm: TelephonyManager, info: EuiccChannelInfo): TelephonyManagerChannel? {
            try {
                val channel = tm.iccOpenLogicalChannelBySlot(info.slotId, EUICC_APP_ID, 0)
                if (channel.status != IccOpenLogicalChannelResponse.STATUS_NO_ERROR || channel.channel == IccOpenLogicalChannelResponse.INVALID_CHANNEL) {
                    Log.e(TAG, "Unable to open eUICC channel for slot ${info.slotId} via TelephonyManager: ${channel.status}")
                    return null
                }

                Log.d(TAG, "channel: ${channel.channel}")

                return TelephonyManagerChannel(info, tm, channel.channel)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open eUICC channel for slot ${info.slotId} via TelephonyManager")
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            }
        }
    }

    override val lpa: LocalProfileAssistant by lazy {
        LocalProfileAssistantImpl(TelephonyManagerApduChannel(tm, slotId, channelId))
    }
    override val valid: Boolean
        get() = true // TODO: Fix this

    override fun close() {
        tm.iccCloseLogicalChannelBySlot(slotId, channelId)
    }
}