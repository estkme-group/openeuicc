package im.angry.openeuicc.core

import android.telephony.TelephonyManager
import com.truphone.lpa.ApduChannel
import com.truphone.lpa.ApduTransmittedListener
import im.angry.openeuicc.util.*

class TelephonyManagerApduChannel(
    private val tm: TelephonyManager,
    private val slotId: Int,
    private val channelId: Int) : ApduChannel {

    override fun transmitAPDU(apdu: String): String? {
        val cla = Integer.parseInt(apdu.substring(0, 2), 16)
        val instruction = Integer.parseInt(apdu.substring(2, 4), 16)
        val p1 = Integer.parseInt(apdu.substring(4, 6), 16)
        val p2 = Integer.parseInt(apdu.substring(6, 8), 16)
        val p3 = Integer.parseInt(apdu.substring(8, 10), 16)
        val p4 = apdu.substring(10)

        return tm.iccTransmitApduLogicalChannelBySlot(
            slotId, channelId,
            cla, instruction, p1, p2, p3, p4)
    }

    override fun transmitAPDUS(apdus: MutableList<String>): String? {
        var res: String? = ""
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