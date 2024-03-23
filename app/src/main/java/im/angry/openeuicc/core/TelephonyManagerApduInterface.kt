package im.angry.openeuicc.core

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.TelephonyManager
import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.ApduInterface

class TelephonyManagerApduInterface(
    private val port: UiccPortInfoCompat,
    private val tm: TelephonyManager
): ApduInterface {
    private var lastChannel: Int = -1

    override val valid: Boolean
        // TelephonyManager channels will never become truly "invalid",
        // just that transactions might return errors or nonsense
        get() = lastChannel != -1

    override fun connect() {
        // Do nothing
    }

    override fun disconnect() {
        // Do nothing
        lastChannel = -1
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        check(lastChannel == -1) { "Already initialized" }
        val hex = aid.encodeHex()
        val channel = tm.iccOpenLogicalChannelByPortCompat(port.card.physicalSlotIndex, port.portIndex, hex, 0)
        if (channel.status != IccOpenLogicalChannelResponse.STATUS_NO_ERROR || channel.channel == IccOpenLogicalChannelResponse.INVALID_CHANNEL) {
            throw IllegalArgumentException("Cannot open logical channel $hex via TelephonManager on slot ${port.card.physicalSlotIndex} port ${port.portIndex}");
        }
        lastChannel = channel.channel
        return lastChannel
    }

    override fun logicalChannelClose(handle: Int) {
        check(handle == lastChannel) { "Invalid channel handle " }
        tm.iccCloseLogicalChannelByPortCompat(port.card.physicalSlotIndex, port.portIndex, handle)
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

        return tm.iccTransmitApduLogicalChannelByPortCompat(port.card.physicalSlotIndex, port.portIndex, lastChannel,
            cla, instruction, p1, p2, p3, p4)?.decodeHex() ?: byteArrayOf()
    }

}