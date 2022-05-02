package im.angry.openeuicc.core

import android.se.omapi.Channel
import android.se.omapi.SEService
import android.util.Log
import com.truphone.lpa.ApduChannel
import com.truphone.lpa.ApduTransmittedListener
import im.angry.openeuicc.util.byteArrayToHex
import im.angry.openeuicc.util.hexStringToByteArray
import java.lang.Exception

class OmapiApduChannel(private val channel: Channel) : ApduChannel {
    companion object {
        private const val TAG = "OmapiApduChannel"
        private val APPLET_ID = byteArrayOf(-96, 0, 0, 5, 89, 16, 16, -1, -1, -1, -1, -119, 0, 0, 1, 0)

        fun tryConnectUiccSlot(service: SEService, slotId: Int): Pair<ApduChannel, EuiccChannelStateManager>? {
            try {
                val reader = service.getUiccReader(slotId + 1) // slotId from telephony starts from 0
                val session = reader.openSession()
                val channel = session.openLogicalChannel(APPLET_ID) ?: return null
                val stateManager = object : EuiccChannelStateManager {
                    override val valid: Boolean
                        get() = channel.isOpen

                    override fun destroy() {
                        channel.close()
                    }
                }
                return Pair(OmapiApduChannel(channel), stateManager)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open eUICC channel for slot ${slotId}, skipping")
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            }
        }
    }

    override fun transmitAPDU(apdu: String): String =
        byteArrayToHex(channel.transmit(hexStringToByteArray(apdu)))

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