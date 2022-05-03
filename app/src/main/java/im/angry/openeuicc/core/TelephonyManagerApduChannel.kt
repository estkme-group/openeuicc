package im.angry.openeuicc.core

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.IccOpenLogicalChannelResponse.INVALID_CHANNEL
import android.telephony.IccOpenLogicalChannelResponse.STATUS_NO_ERROR
import android.telephony.TelephonyManager
import android.util.Log
import com.truphone.lpa.ApduChannel
import com.truphone.lpa.ApduTransmittedListener
import java.lang.Exception
import java.lang.reflect.Method

class TelephonyManagerApduChannel(
    private val tm: TelephonyManager,
    private val slotId: Int,
    private val channelId: Int) : ApduChannel {

    companion object {
        private const val TAG = "TelephonyManagerApduChannel"
        private const val EUICC_APP_ID = "A0000005591010FFFFFFFF8900000100"

        private val iccOpenLogicalChannelBySlot: Method =
            TelephonyManager::class.java.getMethod("iccOpenLogicalChannelBySlot",
                Int::class.java, String::class.java, Int::class.java)
        private val iccCloseLogicalChannelBySlot: Method =
            TelephonyManager::class.java.getMethod("iccCloseLogicalChannelBySlot",
                Int::class.java, Int::class.java)
        private val iccTransmitApduLogicalChannelBySlot: Method =
            TelephonyManager::class.java.getMethod("iccTransmitApduLogicalChannelBySlot",
                Int::class.java, Int::class.java, Int::class.java, Int::class.java,
                Int::class.java, Int::class.java, Int::class.java, String::class.java)

        // TODO: On Tiramisu, we need to specify the portId also if we want MEP support
        fun tryConnectUiccSlot(tm: TelephonyManager, slotId: Int): Pair<ApduChannel, EuiccChannelStateManager>? {
            try {
                val channel = iccOpenLogicalChannelBySlot.invoke(tm, slotId, EUICC_APP_ID, 0) as IccOpenLogicalChannelResponse
                if (channel.status != STATUS_NO_ERROR || channel.channel == INVALID_CHANNEL) {
                    Log.e(TAG, "Unable to open eUICC channel for slot ${slotId} via TelephonyManager")
                    return null
                }

                val stateManager = object : EuiccChannelStateManager {
                    override val valid: Boolean
                        get() = true // TODO: Fix this properly

                    override fun destroy() {
                        iccCloseLogicalChannelBySlot.invoke(tm, slotId, channel.channel)
                    }

                }

                return Pair(TelephonyManagerApduChannel(tm, slotId, channel.channel), stateManager)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open eUICC channel for slot ${slotId} via TelephonyManager")
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            }
        }
    }

    override fun transmitAPDU(apdu: String): String {
        val cla = Integer.parseInt(apdu.substring(0, 2), 16)
        val instruction = Integer.parseInt(apdu.substring(2, 4), 16)
        val p1 = Integer.parseInt(apdu.substring(4, 6), 16)
        val p2 = Integer.parseInt(apdu.substring(6, 8), 16)
        val p3 = Integer.parseInt(apdu.substring(8, 10), 16)
        val p4 = apdu.substring(10)

        return iccTransmitApduLogicalChannelBySlot.invoke(
            tm, slotId, channelId,
            cla, instruction, p1, p2, p3, p4) as String
    }

    override fun transmitAPDUS(apdus: MutableList<String>): String {
        var res = ""
        for (pdu in apdus) {
            res = transmitAPDU(pdu)
        }
        return res
    }

    override fun sendStatus() {
    }

    override fun setApduTransmittedListener(apduTransmittedListener: ApduTransmittedListener?) {
    }

    override fun removeApduTransmittedListener(apduTransmittedListener: ApduTransmittedListener?) {
    }
}