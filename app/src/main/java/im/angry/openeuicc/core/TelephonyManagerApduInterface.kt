package im.angry.openeuicc.core

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.TelephonyManager
import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class TelephonyManagerApduInterface(
    private val info: EuiccChannelInfo,
    private val tm: TelephonyManager
): ApduInterface {
    private var lastChannel: Int = -1

    override fun connect() {
        // Do nothing
    }

    override fun disconnect() {
        // Do nothing
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        check(lastChannel == -1) { "Already initialized" }
        val hex = aid.encodeHex()
        val channel = tm.iccOpenLogicalChannelBySlot(info.slotId, hex, 0)
        if (channel.status != IccOpenLogicalChannelResponse.STATUS_NO_ERROR || channel.channel == IccOpenLogicalChannelResponse.INVALID_CHANNEL) {
            throw IllegalArgumentException("Cannot open logical channel " + hex + " via TelephonManager on slot " + info.slotId);
        }
        lastChannel = channel.channel
        return lastChannel
    }

    override fun logicalChannelClose(handle: Int) {
        check(handle == lastChannel) { "Invalid channel handle " }
        tm.iccCloseLogicalChannelBySlot(info.slotId, handle)
        lastChannel = -1
    }

    override fun transmit(tx: ByteArray): ByteArray {
        check(lastChannel != -1) { "Uninitialized" }

        val cla = tx[0].toUByte().toInt()
        val instruction = tx[1].toUByte().toInt()
        val p1 = tx[2].toUByte().toInt()
        val p2 = tx[3].toUByte().toInt()
        val p3 = tx[4].toUByte().toInt()
        val p4 = tx.drop(5).toByteArray().encodeHex()

        return tm.iccTransmitApduLogicalChannelBySlot(info.slotId, lastChannel,
            cla, instruction, p1, p2, p3, p4)?.decodeHex() ?: byteArrayOf()
    }

}

class TelephonyManagerChannel(
    info: EuiccChannelInfo,
    private val tm: TelephonyManager
) : EuiccChannel(info) {
    override val lpa: LocalProfileAssistant = LocalProfileAssistantImpl(
        TelephonyManagerApduInterface(info, tm),
        HttpInterfaceImpl()
    )
}