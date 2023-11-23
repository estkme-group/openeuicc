package im.angry.openeuicc.core

import android.telephony.TelephonyManager
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class TelephonyManagerApduInterface(
    private val info: EuiccChannelInfo,
    private val tm: TelephonyManager
): ApduInterface {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun logicalChannelClose(handle: Int) {
        TODO("Not yet implemented")
    }

    override fun transmit(tx: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

}

class TelephonyManagerChannel(
    info: EuiccChannelInfo,
    private val tm: TelephonyManager
) : EuiccChannel(info) {
    companion object {
        private const val TAG = "TelephonyManagerApduChannel"
        private const val EUICC_APP_ID = "A0000005591010FFFFFFFF8900000100"

        // TODO: On Tiramisu, we need to specify the portId also if we want MEP support
        /*fun tryConnect(tm: TelephonyManager, info: EuiccChannelInfo): TelephonyManagerChannel? {
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
        }*/
    }

    override val lpa: LocalProfileAssistant = LocalProfileAssistantImpl(
        TelephonyManagerApduInterface(info, tm),
        HttpInterfaceImpl()
    )
    override val valid: Boolean
        get() = true // TODO: Fix this

    override fun close() = lpa.close()
}