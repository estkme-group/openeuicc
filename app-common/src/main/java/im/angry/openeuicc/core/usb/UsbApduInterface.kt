package im.angry.openeuicc.core.usb

import android.util.Log
import im.angry.openeuicc.core.ApduInterfaceAtrProvider
import im.angry.openeuicc.util.decodeHex
import im.angry.openeuicc.util.encodeHex
import net.typeblog.lpac_jni.ApduInterface

class UsbApduInterface(
    private val ccidCtx: UsbCcidContext
) : ApduInterface, ApduInterfaceAtrProvider {
    companion object {
        private const val TAG = "UsbApduInterface"
    }

    override val atr: ByteArray?
        get() = ccidCtx.atr

    override val valid: Boolean
        get() = channels.isNotEmpty()

    private var channels = mutableSetOf<Int>()

    override fun connect() {
        ccidCtx.connect()

        // Send Terminal Capabilities
        // Specs: ETSI TS 102 221 v15.0.0 - 11.1.19 TERMINAL CAPABILITY
        val terminalCapabilities = buildCmd(
            0x80.toByte(), 0xaa.toByte(), 0x00, 0x00,
            "A9088100820101830107".decodeHex(),
            le = null,
        )
        transmitApduByChannel(terminalCapabilities, 0)
    }

    override fun disconnect() = ccidCtx.disconnect()

    override fun logicalChannelOpen(aid: ByteArray): Int {
        // OPEN LOGICAL CHANNEL
        val req = manageChannelCmd(true, 0)

        val resp = try {
            transmitApduByChannel(req, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }

        if (!isSuccessResponse(resp)) {
            Log.d(TAG, "OPEN LOGICAL CHANNEL failed: ${resp.encodeHex()}")
            return -1
        }

        val channelId = resp[0].toInt()
        Log.d(TAG, "channelId = $channelId")
        channels.add(channelId)

        // Then, select AID
        val selectAid = selectByDfCmd(aid, channelId.toByte())
        val selectAidResp = transmitApduByChannel(selectAid, channelId.toByte())

        if (!isSuccessResponse(selectAidResp)) {
            Log.d(TAG, "Select DF failed : ${selectAidResp.encodeHex()}")
            logicalChannelClose(channelId)
            Log.d(TAG, "Closed logical channel $channelId due to select DF failure")
            return -1
        }

        return channelId
    }

    override fun logicalChannelClose(handle: Int) {
        check(channels.contains(handle)) {
            "Invalid logical channel handle $handle"
        }
        // CLOSE LOGICAL CHANNEL
        val req = manageChannelCmd(false, handle.toByte())
        val resp = transmitApduByChannel(req, handle.toByte())

        if (!isSuccessResponse(resp)) {
            Log.d(TAG, "CLOSE LOGICAL CHANNEL failed: ${resp.encodeHex()}")
        }
        channels.remove(handle)
    }

    override fun transmit(handle: Int, tx: ByteArray): ByteArray {
        check(channels.contains(handle)) {
            "Invalid logical channel handle $handle"
        }
        return transmitApduByChannel(tx, handle.toByte())
    }

    private fun isSuccessResponse(resp: ByteArray): Boolean =
        resp.size >= 2 && resp[resp.size - 2] == 0x90.toByte() && resp[resp.size - 1] == 0x00.toByte()

    private fun buildCmd(cla: Byte, ins: Byte, p1: Byte, p2: Byte, data: ByteArray?, le: Byte?) =
        byteArrayOf(cla, ins, p1, p2).let {
            if (data != null) {
                it + data.size.toByte() + data
            } else {
                it
            }
        }.let {
            if (le != null) {
                it + byteArrayOf(le)
            } else {
                it
            }
        }

    private fun manageChannelCmd(open: Boolean, channel: Byte) =
        if (open) {
            buildCmd(0x00, 0x70, 0x00, 0x00, null, 0x01)
        } else {
            buildCmd(channel, 0x70, 0x80.toByte(), channel, null, null)
        }

    private fun selectByDfCmd(aid: ByteArray, channel: Byte) =
        buildCmd(channel, 0xA4.toByte(), 0x04, 0x00, aid, null)

    private fun transmitApduByChannel(tx: ByteArray, channel: Byte): ByteArray {
        val realTx = tx.copyOf()
        // OR the channel mask into the CLA byte
        realTx[0] = ((realTx[0].toInt() and 0xFC) or channel.toInt()).toByte()

        var resp = ccidCtx.transceiver.sendXfrBlock(realTx).data!!

        if (resp.size < 2) throw RuntimeException("APDU response smaller than 2 (sw1 + sw2)!")

        var sw1 = resp[resp.size - 2].toInt() and 0xFF
        var sw2 = resp[resp.size - 1].toInt() and 0xFF

        if (sw1 == 0x6C) {
            // 0x6C = wrong le
            // so we fix the le field here
            realTx[realTx.size - 1] = resp[resp.size - 1]
            resp = ccidCtx.transceiver.sendXfrBlock(realTx).data!!
        } else if (sw1 == 0x61) {
            // 0x61 = X bytes available
            // continue reading by GET RESPONSE
            do {
                // GET RESPONSE
                val getResponseCmd = byteArrayOf(
                    realTx[0], 0xC0.toByte(), 0x00, 0x00, sw2.toByte()
                )

                val tmp = ccidCtx.transceiver.sendXfrBlock(getResponseCmd).data!!

                resp = resp.sliceArray(0 until (resp.size - 2)) + tmp

                sw1 = resp[resp.size - 2].toInt() and 0xFF
                sw2 = resp[resp.size - 1].toInt() and 0xFF
            } while (sw1 == 0x61)
        }

        return resp
    }
}
